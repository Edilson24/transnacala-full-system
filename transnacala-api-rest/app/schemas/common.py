from enum import Enum

from typing import Generic, TypeVar

from pydantic import BaseModel, ConfigDict, Field

T = TypeVar("T")


class UserRole(str, Enum):
    ADMIN = "ADMIN"
    DRIVER = "DRIVER"
    PASSENGER = "PASSENGER"


class ErrorField(BaseModel):
    field: str
    message: str
    code: str


class ErrorDetail(BaseModel):
    code: str
    message: str
    fields: list[ErrorField] = Field(default_factory=list)


class ErrorEnvelope(BaseModel):
    error: ErrorDetail


class PageMetadata(BaseModel):
    model_config = ConfigDict(extra="forbid")

    limit: int = Field(ge=1, le=100)
    offset: int = Field(ge=0)
    total: int = Field(ge=0)


class Page(BaseModel, Generic[T]):
    model_config = ConfigDict(extra="forbid")

    items: list[T]
    page: PageMetadata
