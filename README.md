# LMS Authentication and Employee Backend

Member 1 backend extracted from `E:\eclipse-workspace\Leave-Management-System` into
`E:\eclipse-workspace\lms-auth-employee`. Uses the original `com.nexturn.lms` package,
employee fields and roles. Java 17, Spring Boot 3.4.5, Maven, JPA, MySQL and BCrypt.

## Included scope

| Functional requirement | Implementation |
|---|---|
| Login using BCrypt | `LoginController`, `LoginService`, `LoginServiceImpl` |
| Create a login user | `POST /api/login/user`, `CreateUserRequest` |
| Add/update/view/delete employees | `EmployeeController`, `EmployeeService`, `EmployeeServiceImpl` |
| List managers | `GET /api/employees/managers` |
| View a manager's team | `GET /api/employees/{id}/team` (direct reports) |
| Persistence | `LoginRepository`, `EmployeeRepository`, `Login`, `Employee`, `Role` |
| Central errors | `GlobalExceptionHandler`, `ApiError`, three business exceptions |
| Verification | `LoginServiceTest`, `EmployeeServiceTest`, MVC/JPA integration tests and Lambda adapter test |

Leave applications, balances, holidays, approvals, notifications, audit services and the
old frontend are excluded. The original project is untouched. The starter ping endpoint,
ping tests and duplicate Gradle build were removed. Eclipse metadata is preserved.

All maintained source/configuration files have purpose comments. There are no `try/catch`
blocks in application or test source. MVC/security errors use the global advice; Lambda
startup and stream I/O failures propagate to the runtime because they occur outside MVC.

## Run locally

1. Install/select JDK 17 and Maven 3.9+. Eclipse: Import > Existing Maven Projects, select
   this folder, then Maven > Update Project; choose JDK 17 for the project runtime.
2. Create a **new** MySQL database (do not point this service at the old LMS schema):

   ```sql
   CREATE DATABASE lms_auth_employee CHARACTER SET utf8mb4;
   ```

3. In PowerShell set the local environment. Replace the placeholders privately:

   ```powershell
   $env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
   $env:SPRING_PROFILES_ACTIVE = 'local'
   $env:DB_URL = 'jdbc:mysql://localhost:3306/lms_auth_employee'
   $env:DB_USERNAME = 'your_database_user'
   $env:DB_PASSWORD = 'your_database_password'
   $env:BOOTSTRAP_ADMIN_USERNAME = 'admin@example.com'
   $env:BOOTSTRAP_ADMIN_PASSWORD = 'replace-with-a-strong-password'
   mvn clean verify
   mvn spring-boot:run
   ```

   If Maven is not on PATH, use `& 'E:\apache-maven-3.9.16\bin\mvn.cmd'` in place of `mvn`.
   API base: `http://localhost:8080`. The local profile creates/updates development tables.
   Bootstrap runs only when the login table is empty and both variables are configured;
   there is no default administrator. Use at least 12 password characters and at most
   72 UTF-8 bytes. Remove the bootstrap variables after the first successful startup.

4. For a persistent non-development database, apply `database/schema.sql` once to a fresh
   database and omit the `local` profile. The default profile validates the schema and
   does not auto-create or update production tables. Future schema changes require an
   explicit reviewed migration. Tests use H2 only and never use your MySQL credentials.

## Authentication and authorization

`POST /api/login` verifies credentials and returns `username` and `role`. It does **not**
issue a JWT, establish a server session or return an access token. Protected requests
must send `Authorization: Basic <base64(username:password)>` each time. Use HTTPS in AWS.
There is no public self-registration; HR/ADMIN creates profiles and accounts.

| Operation | Access |
|---|---|
| Get CSRF token; submit login | Public, with CSRF token for login POST |
| Create login users | ADMIN: any role; HR: EMPLOYEE or MANAGER only |
| Create, list, update, delete employees | HR or ADMIN |
| View one employee | HR/ADMIN or the employee owning that email identity |
| List managers | Any authenticated user; returns only ID, name, email, designation |
| View manager team | HR/ADMIN or the manager owning that profile |

HTTP Basic credentials may be resent by browsers, so CSRF protection remains enabled.
First call `GET /api/csrf`; retain the `XSRF-TOKEN` cookie and send its token value in the
`X-XSRF-TOKEN` header on POST/PUT/DELETE, including login. GET requests need no CSRF header.
Cross-origin access is not enabled by default; configure a specific trusted frontend
origin when a frontend is added. Do not enable unrestricted credentialed CORS.

## Endpoints

| Method | Path | Success |
|---|---|---|
| GET | `/api/csrf` | 200 token, headerName, parameterName; sets cookie |
| POST | `/api/login` | 200 username, role |
| POST | `/api/login/user` | 201 username, role |
| GET | `/api/employees` | 200 employee list |
| GET | `/api/employees/{id}` | 200 employee |
| POST | `/api/employees` | 201 employee and Location header |
| PUT | `/api/employees/{id}` | 200 updated employee (complete replacement input) |
| DELETE | `/api/employees/{id}` | 204, empty body |
| GET | `/api/employees/managers` | 200 manager directory |
| GET | `/api/employees/{id}/team` | 200 direct-report employees |

Sample login body:

```json
{"username":"admin@example.com","password":"replace-with-a-strong-password"}
```

Sample employee body (POST and PUT):

```json
{
  "empName": "Asha Rao",
  "email": "asha@example.com",
  "phoneNumber": "9876543210",
  "designation": "Software Engineer",
  "deptId": 1,
  "managerId": null,
  "gender": "Female",
  "address": "Pune"
}
```

Sample login-user creation body, **after** creating the matching employee profile:

```json
{"username":"asha@example.com","password":"a-strong-user-password","role":"EMPLOYEE"}
```

PowerShell example retaining the CSRF cookie (requires the application to be running):

```powershell
$base = 'http://localhost:8080'
$csrf = Invoke-RestMethod "$base/api/csrf" -SessionVariable lmsSession
$headers = @{ 'X-XSRF-TOKEN' = $csrf.token }
$credential = Get-Credential 'admin@example.com'
$body = @{ username = $credential.UserName; password = $credential.GetNetworkCredential().Password } | ConvertTo-Json
Invoke-RestMethod "$base/api/login" -Method Post -WebSession $lmsSession -Headers $headers -ContentType 'application/json' -Body $body
# Add HTTP Basic for protected operations; never commit this generated header.
$bytes = [Text.Encoding]::UTF8.GetBytes("$($credential.UserName):$($credential.GetNetworkCredential().Password)")
$headers.Authorization = 'Basic ' + [Convert]::ToBase64String($bytes)
Invoke-RestMethod "$base/api/employees" -WebSession $lmsSession -Headers $headers
# Submit the employee JSON above using POST with the same session and headers.
```

In Postman, first GET `/api/csrf`, retain the cookie, add `X-XSRF-TOKEN` for writes and use
the Authorization > Basic Auth tab on protected requests. Then create a manager profile,
create its login user with role MANAGER, and assign that returned employee ID as the
`managerId` when creating team members. Profile creation and login creation are separate
operations; the bootstrap administrator is the only account that need not have a profile.

## Data and validation rules

- Employee IDs use database identity allocation. Never calculate `MAX(id) + 1` and do not
  supply IDs in create/update bodies. IDs are not guaranteed to start at the old value 1000.
- Usernames and employee emails are trimmed/lowercased. Usernames are the corresponding
  employee email; duplicate accounts never replace an existing password.
- Passwords are BCrypt hashes at cost 10. Creation requires at least 8 characters, at most
  72 characters and at most 72 UTF-8 bytes. Responses and DTO string representations do
  not expose passwords. No plaintext-password migration is performed.
- Employee name, email, 10-digit phone, designation, gender and address are required.
  Department/manager IDs are optional positive integers. Department validation belongs to
  the separate department module and is not included.
- Email and phone must be unique. A manager must have an employee profile plus a MANAGER
  login role. Self-management and cycles in the reporting chain are rejected.
- Email cannot change while a matching login account exists, to keep the identity link
  consistent. There is no role-change, account-password-change or standalone account-delete
  API in this assigned scope.
- Delete removes the matching login and employee together. Managers with direct reports
  must have their reports reassigned first. Production SQL also enforces the manager FK.

## Global errors

| Status | Meaning |
|---|---|
| 400 | Invalid JSON, role, field validation or manager relationship |
| 401 | Missing/invalid authentication or wrong login credentials |
| 403 | Role/ownership failure or missing/invalid CSRF token |
| 404 | Missing employee, manager profile or endpoint |
| 405 | Unsupported HTTP method |
| 409 | Duplicate contact/account, linked-email change or manager with reports |
| 500 | Unexpected failure; generic client message, diagnostic server log |

Example response:

```json
{
  "timestamp": "2026-10-28T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/api/employees",
  "fieldErrors": {"email": "must not be blank"}
}
```

Security-filter errors are delegated to the same MVC global handler. Database uniqueness
constraints provide a final duplicate check if concurrent requests pass pre-checks.

## File responsibilities

`src/main/java/com/nexturn/lms/` contains:

- `Application`, `StreamLambdaHandler`: local application and AWS REST-event bridge.
- `controller/`: HTTP routing, input validation, access rules and safe response DTOs.
- `service/`: required service interfaces/implementations, BCrypt and employee rules.
- `repository/`: the two JPA repositories only.
- `entity/`, `enums/`: the two retained data models and original Role enum.
- `dto/`: credential/profile inputs, safe responses and shared error format.
- `exception/`: centralized handler and business/missing/conflict exceptions.
- `config/`: security, ownership lookup and initial administrator bootstrap.

`src/main/resources/`: environment-driven configuration and optional local profile.
`src/test/`: service behavior, HTTP/security/JPA and Lambda bridge verification.
`database/schema.sql`: initial production MySQL schema.
`src/assembly/bin.xml`: Lambda zip layout. `template.yml`: SAM infrastructure template.

## Build and verify

```powershell
mvn clean verify
```

Outputs in `target/`:

- `lms-auth-employee-0.0.1-SNAPSHOT-exec.jar`: runnable local application.
- `lms-auth-employee-0.0.1-SNAPSHOT-lambda-package.zip`: Lambda classes and runtime jars.
- `surefire-reports/`: test results.

Local jar alternative: `java -jar target/lms-auth-employee-0.0.1-SNAPSHOT-exec.jar` with the
same database/profile environment. Rebuild with `clean` when changing runtime dependencies
so old jars do not remain in the Lambda package.

## AWS preparation (deployment is a later step)

The starter already targets Lambda + API Gateway REST API. This project retains that
architecture and includes a SAM template. It does not create or deploy AWS resources now.

1. Choose a region, configure a local AWS CLI profile through your approved sign-in method,
   and install AWS SAM CLI. Keep AWS access keys and database secrets out of chat/source.
2. Prepare a dedicated MySQL 8/RDS database and run `database/schema.sql`. Use TLS with
   `sslMode=VERIFY_IDENTITY` and trusted certificates as appropriate for the RDS endpoint.
3. Supply private subnet IDs and Lambda security-group IDs in the same VPC as the database.
   Permit database ingress on port 3306 from the Lambda security group. The template does
   not create a database, VPC, subnets or networking rules.
4. Build with `mvn clean verify`, then validate with `sam validate --lint --template-file template.yml`.
   The template uses the prebuilt zip in `target/`; do not deploy the executable Boot jar.
5. At the deployment stage, use `sam deploy --guided --template-file template.yml --profile YOUR_PROFILE`.
   Supply the database URL/user/password, subnets, security groups and optional bootstrap
   administrator values privately. Review the CloudFormation change set before execution.
   Parameters marked `NoEcho` are hidden in normal parameter displays; restrict access to
   Lambda environment configuration. Do not save secret parameter overrides in Git.
6. After initial provisioning, remove the bootstrap variables and redeploy. Invoke the
   HTTPS API output's `/api/csrf`, then login and run the documented authenticated workflow.

The template caps reserved concurrency at 5 with at most 3 DB connections per environment;
confirm regional concurrency quota and database connection capacity before deployment.
Tune these together or consider RDS Proxy for larger concurrency. Cold-start timing and
real RDS connectivity must be checked in AWS; local tests cannot establish those results.

Official references used for the adapter, packaging and CSRF choices:

- [AWS Serverless Java Container Spring Boot 3 guide](https://github.com/aws/serverless-java-container/wiki/Quick-start---Spring-Boot3)
- [AWS SAM Function reference](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-resource-function.html)
- [Spring Security CSRF reference](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)

## Verification completed

Maven `clean verify` passed with 48 JUnit tests, zero failures, zero errors and zero skipped tests. Tests use isolated H2 databases; real MySQL/RDS connectivity and AWS deployment remain to be verified during environment setup. Mockito uses subclass mocks and reflection access to avoid Windows agent-attachment restrictions.

## Project-specific AWS deployment update

The complete infrastructure definition is `deployment/aws-stack.json`. It replaces the older starter-only deployment workflow when creating new resources. All three Lambda functions specify Java 17; Maven compiles application classes with release 17. Local verification ran on the installed JDK 21, not a locally installed JDK 17.

- Main function: `leave-management-system-lambda-aws`; handler `com.nexturn.lms.StreamLambdaHandler::handleRequest`.
- API Gateway: `leave-management-system-api-aws`, REST proxy `ANY /{proxy+}`, stage `stage1`.
- RDS: `leave-management-system-rds-aws`, private single-AZ MySQL database `lms_auth_employee`.
- Setup function: `leave-management-system-db-setup-aws`; invoke explicitly once with `{ "initialize": true }` after stack creation.
- Relay function: `leave-management-system-event-relay-aws`; publishes pending employee events to SNS. Enable its schedule only after schema setup.
- SNS topic and SQS queue: `leave-management-system-employee-events`. SQS retains events for later consumption; this scope does not include an event consumer.
- Dead-letter queue: `leave-management-system-employee-events-dlq`.
- Frontend bucket: `leave-management-system-frontend-<account-id>-<region>`. Upload the three files in `frontend/` and use the CloudFront HTTPS output URL.
- Supporting resources: private VPC/subnets, security groups, one SNS VPC endpoint, Lambda roles, log groups, generated database/admin secrets, CloudFront policies and S3 origin access control.

Employee changes now write an outbox event in the same database transaction. The relay retries pending records; messages are at least once and consumers must deduplicate by `eventId`. Events contain an employee ID and operation only. RDS must have the added `employee_event` table before running the updated backend. Apply the full initial schema only to this new dedicated database. Existing deployments require a reviewed migration for the new table.

The frontend copies and adapts only the required original React login, employee and team screens and their shared files. It supports login, role-based profile CRUD, account creation and team lookup. Build it with npm ci and npm run build; upload frontend/dist contents to S3. `GET /api/employees/me` supports employee/manager self-profile lookup. Credentials remain in tab memory. There is no public register page and no excluded leave/approval features.

Private S3 plus CloudFront supplies HTTPS and a same-origin API path for the CSRF cookie. This replaces the reference PDF's public HTTP S3 website. IAM roles replace embedded access keys, generated secrets replace sample passwords, and private RDS access replaces a public 3306 rule. The reference's example account IDs, queue ARNs and resource names are never reused.

Deployment status: PREPARED, NOT DEPLOYED. AWS sign-in/MFA, region, spending limit, final stack review and live verification are still required. No deployed URL exists yet. Resource names above are planned names until creation succeeds.

Frontend verification: production build passed, dependency audit reported zero vulnerabilities, and local browser tests verified login, employee creation and employee update against a disposable H2 database. Deployment is still pending AWS authentication and configuration.

Success messaging: ACCOUNT_CREATED and LOGIN_SUCCEEDED join employee CREATED/UPDATED/DELETED events. The app displays success messages; the outbox relay publishes safe metadata and a message to SNS/SQS. An optional SNS email subscription is configured through the NotificationEmail deployment parameter and requires recipient confirmation. An initial administrator login may have employeeId=null.

SNS uses protocol-specific messages: JSON for SQS, readable success notifications with a LeaveHub subject for confirmed email subscribers. Email addresses are deployment parameters, not source constants.
