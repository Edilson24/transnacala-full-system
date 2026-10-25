from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.schemas.catalogue import UserResponse


class LoginRequest(BaseModel):
    username: str = Field(max_length=80)
    password: str = Field(
        min_length=1, json_schema_extra={"format": "password"}
    )

    @field_validator("username")
    @classmethod
    def trim_username(cls, value: str) -> str:
        value = value.strip()
        if not value:
            raise ValueError("Username cannot be blank.")
        return value


class LoginResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    access_token: str
    token_type: str = "bearer"
    expires_in: int = Field(gt=0)
    user: UserResponse
