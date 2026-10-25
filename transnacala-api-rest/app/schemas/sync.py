from datetime import datetime
from enum import Enum
from uuid import UUID

from pydantic import BaseModel, Field


class SyncEntityType(str, Enum):
    ROUTE = "ROUTE"
    STOP = "STOP"
    ROUTE_STOP = "ROUTE_STOP"
    VEHICLE = "VEHICLE"


class SyncOperation(str, Enum):
    UPSERT = "UPSERT"
    DELETE = "DELETE"


class SyncChange(BaseModel):
    id: int = Field(ge=1)
    entity_type: SyncEntityType
    entity_id: UUID
    related_entity_id: UUID | None = None
    operation: SyncOperation
    changed_at: datetime


class SyncChangePage(BaseModel):
    items: list[SyncChange]
    since_id: int = Field(ge=0)
    next_since_id: int = Field(ge=0)
    has_more: bool
