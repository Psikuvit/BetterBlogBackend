# BetterBlog Password Reset

This backend now supports a password-reset flow that sends a one-time code to the user's email.

## Endpoints

### Request reset code
`POST /api/auth/forgot-password`

Request body:
```json
{ "email": "user@example.com" }
```

Response:
```json
{ "message": "If an account with that email exists, a reset code has been sent.", "expiresInMinutes": 30 }
```

### Reset password
`POST /api/auth/reset-password`

Request body:
```json
{
  "email": "user@example.com",
  "code": "123456",
  "newPassword": "NewStrongPassword123!"
}
```

## Code expiry
- Reset codes are valid for **30 minutes** by default.
- The TTL can be overridden with `PASSWORD_RESET_CODE_TTL_MINUTES`.

## SMTP environment variables
Set these in Render or your local environment:

- `SPRING_MAIL_HOST`
- `SPRING_MAIL_PORT`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`
- `SPRING_MAIL_SMTP_AUTH`
- `SPRING_MAIL_SMTP_STARTTLS_ENABLE`
- `MAIL_FROM`

## Local Docker testing
The bundled `docker-compose.yml` includes MailHog so you can test reset emails locally.
- MailHog UI: `http://localhost:8025`

