import re
from uuid import UUID, uuid4

from fastapi import APIRouter, Depends, HTTPException, Query, Request
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy import func, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.api.auth import parse_user_id
from app.db.dependencies import get_db
from app.models.entities import (
    Route,
    RouteStop,
    Stop,
    SyncChange,
    SyncEntityType,
    SyncOperation,
    SyncSequence,
    User,
    UserRole,
    Vehicle,
)
from app.schemas import (
    RoutePage,
    RouteResponse,
    RouteStopInput,
    RouteWrite,
    StopPage,
    StopResponse,
    StopWrite,
    UserCreate,
    UserPage,
    UserResponse,
    UserRole as SchemaUserRole,
    UserUpdate,
    VehicleAssignment,
    VehiclePage,
    VehicleResponse,
    VehicleUpdate,
    SyncChangePage,
)
from app.schemas.catalogue import RouteUpdate, StopUpdate, VehicleWrite
from app.schemas.common import PageMetadata
from app.services.security import hash_password, verify_access_token

router = APIRouter(prefix="/api/v1", tags=["Administration"])
bearer = HTTPBearer(auto_error=False)


def optional_current_user(
    request: Request,
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer),
    session: Session = Depends(get_db),
) -> User | None:
    if credentials is None:
        return None
    claims = verify_access_token(
        credentials.credentials, request.app.state.token_secret
    )
    user_id = parse_user_id(claims.get("sub") if claims else None)
    user = session.get(User, str(user_id)) if user_id else None
    if user is None or not user.is_active:
        raise _unauthorized()
    return user


def current_user(
    user: User | None = Depends(optional_current_user),
) -> User:
    if user is None:
        raise _unauthorized()
    return user


def admin_user(user: User = Depends(current_user)) -> User:
    if user.role != UserRole.ADMIN:
        raise HTTPException(
            status_code=403,
            detail={"code": "FORBIDDEN", "message": "Administrator access is required."},
        )
    return user


def _unauthorized() -> HTTPException:
    return HTTPException(
        status_code=401,
        detail={"code": "UNAUTHORIZED", "message": "A valid access token is required."},
        headers={"WWW-Authenticate": "Bearer"},
    )


def _not_found(entity: str) -> HTTPException:
    return HTTPException(
        status_code=404,
        detail={"code": "NOT_FOUND", "message": f"{entity} was not found."},
    )


def _conflict(message: str) -> HTTPException:
    return HTTPException(
        status_code=409, detail={"code": "CONFLICT", "message": message}
    )


def _require_admin_for_inactive(include_inactive: bool, user: User | None) -> None:
    if not include_inactive:
        return
    if user is None:
        raise _unauthorized()
    if user.role != UserRole.ADMIN:
        raise HTTPException(
            status_code=403,
            detail={"code": "FORBIDDEN", "message": "Administrator access is required."},
        )


def _reject_null_values(changes: dict[str, object], allowed: set[str]) -> None:
    null_fields = [name for name, value in changes.items() if value is None and name not in allowed]
    if null_fields:
        raise HTTPException(
            status_code=422,
            detail={
                "code": "VALIDATION_ERROR",
                "message": "The request contains invalid fields.",
                "fields": [
                    {"field": name, "message": "This field cannot be null.", "code": "null"}
                    for name in null_fields
                ],
            },
        )


def _commit(session: Session) -> None:
    try:
        session.commit()
    except IntegrityError as exc:
        session.rollback()
        raise _conflict("A record with the same unique value already exists.") from exc


def lock_sync_sequence(session: Session) -> SyncSequence:
    sequence = session.scalar(
        select(SyncSequence).where(SyncSequence.id == 1).with_for_update()
    )
    if sequence is None:
        sequence = SyncSequence(id=1, last_change_id=0)
        session.add(sequence)
        session.flush()
    return sequence


def record_sync_change(
    session: Session,
    entity_type: SyncEntityType,
    entity_id: str,
    operation: SyncOperation = SyncOperation.UPSERT,
    related_entity_id: str | None = None,
    sequence: SyncSequence | None = None,
) -> None:
    locked_sequence = sequence or lock_sync_sequence(session)
    locked_sequence.last_change_id += 1
    session.add(
        SyncChange(
            id=locked_sequence.last_change_id,
            entity_type=entity_type,
            entity_id=entity_id,
            related_entity_id=related_entity_id,
            operation=operation,
        )
    )


def _page(query, count_query, session: Session, limit: int, offset: int):
    items = session.scalars(query.limit(limit).offset(offset)).all()
    total = session.scalar(count_query) or 0
    return items, PageMetadata(limit=limit, offset=offset, total=total)


def _route_response(session: Session, route: Route) -> RouteResponse:
    stops = session.execute(
        select(
            RouteStop.stop_id,
            Stop.name,
            RouteStop.stop_order,
            Stop.latitude,
            Stop.longitude,
        )
        .join(Stop, Stop.id == RouteStop.stop_id)
        .where(RouteStop.route_id == route.id)
        .order_by(RouteStop.stop_order)
    ).all()
    payload = {
        **RouteResponse.model_validate(route).model_dump(),
        "stops": [
            {
                "stop_id": row.stop_id,
                "name": row.name,
                "stop_order": row.stop_order,
                "latitude": row.latitude,
                "longitude": row.longitude,
            }
            for row in stops
        ],
    }
    return RouteResponse.model_validate(payload)


@router.post("/users", status_code=201, response_model=UserResponse)
def create_user(
    payload: UserCreate,
    session: Session = Depends(get_db),
    _: User = Depends(admin_user),
) -> User:
    user = User(
        id=str(uuid4()),
        username=payload.username.strip(),
        password_hash=hash_password(payload.password),
        full_name=payload.full_name.strip(),
        phone=payload.phone,
        role=UserRole(payload.role.value),
    )
    session.add(user)
    _commit(session)
    session.refresh(user)
    return user


@router.get("/users", response_model=UserPage)
def list_users(
    limit: int = Query(default=50, ge=1, le=100),
    offset: int = Query(default=0, ge=0),
    role: SchemaUserRole | None = None,
    session: Session = Depends(get_db),
    _: User = Depends(admin_user),
) -> UserPage:
    query = select(User)
    count_query = select(func.count()).select_from(User)
    if role is not None:
        query = query.where(User.role == UserRole(role.value))
        count_query = count_query.where(User.role == UserRole(role.value))
    query = query.order_by(User.created_at.desc(), User.id)
    users, page = _page(query, count_query, session, limit, offset)
    return UserPage(items=users, page=page)


@router.get("/users/{user_id}", response_model=UserResponse)
def get_user(
    user_id: UUID,
    session: Session = Depends(get_db),
    _: User = Depends(admin_user),
) -> User:
    user = session.get(User, str(user_id))
    if user is None:
        raise _not_found("User")
    return user


@router.patch("/users/{user_id}", response_model=UserResponse)
def update_user(
    user_id: UUID,
    payload: UserUpdate,
    session: Session = Depends(get_db),
    _: User = Depends(admin_user),
) -> User:
    user = session.get(User, str(user_id))
    if user is None:
        raise _not_found("User")
    changes = payload.model_dump(exclude_unset=True)
    _reject_null_values(changes, {"phone"})
    resulting_role = changes.get("role", user.role)
    resulting_active = changes.get("is_active", user.is_active)
    if user.role == UserRole.ADMIN and user.is_active and (
        resulting_role != UserRole.ADMIN or not resulting_active
    ):
        active_admins = session.scalar(
            select(func.count())
            .select_from(User)
            .where(User.role == UserRole.ADMIN, User.is_active.is_(True))
        ) or 0
        if active_admins <= 1:
            raise _conflict("The last active administrator cannot be deactivated or demoted.")
    if user.role == UserRole.DRIVER and (
        resulting_role != UserRole.DRIVER or not resulting_active
    ):
        assigned_vehicles = session.scalar(
            select(func.count())
            .select_from(Vehicle)
            .where(Vehicle.driver_user_id == user.id)
        ) or 0
        if assigned_vehicles:
            raise _conflict("Unassign this driver from vehicles before deactivating the account or changing its role.")
    for field, value in changes.items():
        if field == "password":
            user.password_hash = hash_password(value)
        else:
            setattr(user, field, UserRole(value.value) if field == "role" else value)
    _commit(session)
    session.refresh(user)
    return user


@router.get("/routes", response_model=RoutePage)
def list_routes(
    limit: int = Query(default=50, ge=1, le=100),
    offset: int = Query(default=0, ge=0),
    include_inactive: bool = False,
    session: Session = Depends(get_db),
    actor: User | None = Depends(optional_current_user),
) -> RoutePage:
    _require_admin_for_inactive(include_inactive, actor)
    query = select(Route).order_by(Route.name, Route.id)
    count_query = select(func.count()).select_from(Route)
    if not include_inactive:
        query = query.where(Route.is_active.is_(True))
        count_query = count_query.where(Route.is_active.is_(True))
    routes, page = _page(query, count_query, session, limit, offset)
    return RoutePage(items=[_route_response(session, route) for route in routes], page=page)


@router.post("/routes", status_code=201, response_model=RouteResponse)
def create_route(
    payload: RouteWrite,
    session: Session = Depends(get_db),
    _: User = Depends(admin_user),
) -> RouteResponse:
    route = Route(id=str(uuid4()), **payload.model_dump())
    session.add(route)
    record_sync_change(session, SyncEntityType.ROUTE, route.id)
    _commit(session)
    session.refresh(route)
    return _route_response(session, route)


@router.get("/routes/{route_id}", response_model=RouteResponse)
def get_route(route_id: UUID, session: Session = Depends(get_db)) -> RouteResponse:
    route = session.get(Route, str(route_id))
    if route is None or not route.is_active:
        raise _not_found("Route")
    return _route_response(session, route)


@router.patch("/routes/{route_id}", response_model=RouteResponse)
def update_route(
    route_id: UUID,
    payload: RouteUpdate,
    session: Session = Depends(get_db),
    _: User = Depends(admin_user),
) -> RouteResponse:
    route = session.get(Route, str(route_id))
    if route is None:
        raise _not_found("Route")
    changes = payload.model_dump(exclude_unset=True)
    _reject_null_values(changes, {"description"})
    for field, value in changes.items():
        setattr(route, field, value)
    record_sync_change(session, SyncEntityType.ROUTE, route.id)
    _commit(session)
    session.refresh(route)
    return _route_response(session, route)


@router.put("/routes/{route_id}/stops", response_model=RouteResponse)
def replace_route_stops(
    route_id: UUID,
    payload: list[RouteStopInput],
    session: Session = Depends(get_db),
    _: User = Depends(admin_user),
) -> RouteResponse:
    route = session.get(Route, str(route_id))
    if route is None:
        raise _not_found("Route")
    ids = [str(item.stop_id) for item in payload]
    orders = [item.stop_order for item in payload]
    if len(set(ids)) != len(ids) or len(set(orders)) != len(orders):
        raise HTTPException(
            status_code=422,
            detail={"code": "VALIDATION_ERROR", "message": "Stop IDs and orders must be unique."},
        )
    stops = session.scalars(select(Stop).where(Stop.id.in_(ids), Stop.is_active.is_(True))).all() if ids else []
    if len(stops) != len(ids):
        raise HTTPException(
            status_code=422,
            detail={"code": "INVALID_STOP", "message": "Every stop must exist and be active."},
        )

    existing = session.scalars(
        select(RouteStop).where(RouteStop.route_id == route.id)
    ).all()
    old_ids = {item.stop_id for item in existing}
    new_ids = set(ids)
    for item in existing:
        session.delete(item)
        if item.stop_id not in new_ids:
            record_sync_change(
                session, SyncEntityType.ROUTE_STOP, route.id,
                SyncOperation.DELETE, item.stop_id,
            )
    session.flush()
    for item in payload:
        stop_id = str(item.stop_id)
        session.add(RouteStop(route_id=route.id, stop_id=stop_id, stop_order=item.stop_order))
        record_sync_change(
            session, SyncEntityType.ROUTE_STOP, route.id,
            SyncOperation.UPSERT, stop_id,
        )
    if not payload and old_ids:
        record_sync_change(session, SyncEntityType.ROUTE, route.id)
    _commit(session)
    session.refresh(route)
    return _route_response(session, route)


@router.post("/routes/{route_id}/stops", status_code=201, response_model=RouteResponse)
def create_route_stop(
    route_id: UUID,
    payload: StopWrite,
    session: Session = Depends(get_db),
    _: User = Depends(admin_user),
) -> RouteResponse:
    route = session.get(Route, str(route_id))
    if route is None:
        raise _not_found("Route")
    stop = Stop(id=str(uuid4()), **payload.model_dump())
    session.add(stop)
    session.flush()
    next_order = (session.scalar(
        select(func.max(RouteStop.stop_order)).where(RouteStop.route_id == route.id)
    ) or 0) + 1
    session.add(RouteStop(route_id=route.id, stop_id=stop.id, stop_order=next_order))
    sequence = lock_sync_sequence(session)
    record_sync_change(session, SyncEntityType.STOP, stop.id, sequence=sequence)
    record_sync_change(
        session, SyncEntityType.ROUTE_STOP, route.id,
        related_entity_id=stop.id, sequence=sequence,
    )
    _commit(session)
    session.refresh(route)
    return _route_response(session, route)


@router.get("/stops", response_model=StopPage)
def list_stops(
    limit: int = Query(default=50, ge=1, le=100),
    offset: int = Query(default=0, ge=0),
    include_inactive: bool = False,
    session: Session = Depends(get_db),
    actor: User | None = Depends(optional_current_user),
) -> StopPage:
    _require_admin_for_inactive(include_inactive, actor)
    query = select(Stop).order_by(Stop.name, Stop.id)
    count_query = select(func.count()).select_from(Stop)
    if not include_inactive:
        query = query.where(Stop.is_active.is_(True))
        count_query = count_query.where(Stop.is_active.is_(True))
    stops, page = _page(query, count_query, session, limit, offset)
    return StopPage(items=stops, page=page)


@router.post("/stops", status_code=201, response_model=StopResponse)
def create_stop(
    payload: StopWrite,
    session: Session = Depends(get_db),
    _: User = Depends(admin_user),
) -> Stop:
    stop = Stop(id=str(uuid4()), **payload.model_dump())
    session.add(stop)
    record_sync_change(session, SyncEntityType.STOP, stop.id)
    _commit(session)
    session.refresh(stop)
    return stop


@router.get("/stops/{stop_id}", response_model=StopResponse)
def get_stop(stop_id: UUID, session: Session = Depends(get_db)) -> Stop:
    stop = session.get(Stop, str(stop_id))
    if stop is None or not stop.is_active:
        raise _not_found("Stop")
    return stop


@router.patch("/stops/{stop_id}", response_model=StopResponse)
def update_stop(
    stop_id: UUID,
    payload: StopUpdate,
    session: Session = Depends(get_db),
    _: User = Depends(admin_user),
) -> Stop:
    stop = session.get(Stop, str(stop_id))
    if stop is None:
        raise _not_found("Stop")
    changes = payload.model_dump(exclude_unset=True)
    _reject_null_values(changes, set())
    for field, value in changes.items():
        setattr(stop, field, value)
    record_sync_change(session, SyncEntityType.STOP, stop.id)
    _commit(session)
    session.refresh(stop)
    return stop


@router.get("/vehicles", response_model=VehiclePage)
def list_vehicles(
    limit: int = Query(default=50, ge=1, le=100),
    offset: int = Query(default=0, ge=0),
    include_inactive: bool = False,
    session: Session = Depends(get_db),
    actor: User | None = Depends(optional_current_user),
) -> VehiclePage:
    _require_admin_for_inactive(include_inactive, actor)
    query = select(Vehicle).order_by(Vehicle.code)
    count_query = select(func.count()).select_from(Vehicle)
    if not include_inactive:
        query = query.where(Vehicle.is_active.is_(True))
        count_query = count_query.where(Vehicle.is_active.is_(True))
    vehicles, page = _page(query, count_query, session, limit, offset)
    return VehiclePage(items=vehicles, page=page)


@router.post("/vehicles", status_code=201, response_model=VehicleResponse)
def create_vehicle(
    payload: VehicleWrite,
    session: Session = Depends(get_db),
    _: User = Depends(admin_user),
) -> Vehicle:
    sequence = lock_sync_sequence(session)
    codes = session.scalars(select(Vehicle.code).where(Vehicle.code.like("V%"))).all()
    last_number = max(
        (int(match.group(1)) for code in codes if (match := re.fullmatch(r"V(\d+)", code))),
        default=0,
    )
    code = f"V{last_number + 1:03d}"
    vehicle = Vehicle(id=str(uuid4()), code=code)
    session.add(vehicle)
    record_sync_change(session, SyncEntityType.VEHICLE, vehicle.id, sequence=sequence)
    _commit(session)
    session.refresh(vehicle)
    return vehicle


@router.get("/vehicles/{vehicle_id}", response_model=VehicleResponse)
def get_vehicle(vehicle_id: UUID, session: Session = Depends(get_db)) -> Vehicle:
    vehicle = session.get(Vehicle, str(vehicle_id))
    if vehicle is None or not vehicle.is_active:
        raise _not_found("Vehicle")
    return vehicle


@router.patch("/vehicles/{vehicle_id}", response_model=VehicleResponse)
def update_vehicle(
    vehicle_id: UUID,
    payload: VehicleUpdate,
    session: Session = Depends(get_db),
    _: User = Depends(admin_user),
) -> Vehicle:
    vehicle = session.get(Vehicle, str(vehicle_id))
    if vehicle is None:
        raise _not_found("Vehicle")
    for field, value in payload.model_dump(exclude_unset=True).items():
        setattr(vehicle, field, value)
    record_sync_change(session, SyncEntityType.VEHICLE, vehicle.id)
    _commit(session)
    session.refresh(vehicle)
    return vehicle


@router.put("/vehicles/{vehicle_id}/assignment", response_model=VehicleResponse)
def assign_vehicle(
    vehicle_id: UUID,
    payload: VehicleAssignment,
    session: Session = Depends(get_db),
    _: User = Depends(admin_user),
) -> Vehicle:
    vehicle = session.get(Vehicle, str(vehicle_id))
    if vehicle is None:
        raise _not_found("Vehicle")
    driver_id = str(payload.driver_user_id) if payload.driver_user_id else None
    route_id = str(payload.route_id) if payload.route_id else None
    if driver_id is not None:
        driver = session.get(User, driver_id)
        if driver is None or driver.role != UserRole.DRIVER or not driver.is_active:
            raise HTTPException(
                status_code=422,
                detail={"code": "INVALID_DRIVER", "message": "An active driver account is required."},
            )
    if route_id is not None:
        route = session.get(Route, route_id)
        if route is None or not route.is_active:
            raise HTTPException(
                status_code=422,
                detail={"code": "INVALID_ROUTE", "message": "An active route is required."},
            )
    vehicle.driver_user_id = driver_id
    vehicle.route_id = route_id
    record_sync_change(session, SyncEntityType.VEHICLE, vehicle.id)
    _commit(session)
    session.refresh(vehicle)
    return vehicle


@router.get("/sync/changes", response_model=SyncChangePage)
def get_sync_changes(
    since_id: int = Query(default=0, ge=0),
    limit: int = Query(default=100, ge=1, le=500),
    session: Session = Depends(get_db),
) -> dict[str, object]:
    rows = session.scalars(
        select(SyncChange)
        .where(SyncChange.id > since_id)
        .order_by(SyncChange.id)
        .limit(limit + 1)
    ).all()
    has_more = len(rows) > limit
    items = rows[:limit]
    return {
        "items": items,
        "since_id": since_id,
        "next_since_id": items[-1].id if items else since_id,
        "has_more": has_more,
    }


@router.api_route(
    "/{path:path}",
    methods=["GET", "POST", "PUT", "PATCH", "DELETE"],
    include_in_schema=False,
)
def pending_domain_endpoint(path: str) -> None:
    raise HTTPException(
        status_code=501,
        detail={
            "code": "DOMAIN_ENDPOINT_NOT_IMPLEMENTED",
            "message": "This domain endpoint is not implemented yet.",
        },
    )
