# LMS authentication and employee frontend

This frontend is inside the new `lms-auth-employee` project. Only the necessary original files were copied and adapted from the previous LMS frontend. The original project remains unchanged.

## Required files retained

- Login, Employees and Team screens: original design/table structure, adapted to this backend.
- App, main, auth and api: only the required routes, tab-memory identity, Basic authentication and CSRF exchange.
- Layout and UI components: original appearance, with unrelated navigation and helpers removed.
- Original custom.css and Bootstrap: shared theme and responsive behavior.
- Profile.jsx: necessary new own-profile screen so employee login does not require a full employee-directory permission.
- package.json, lockfile, Vite config and index.html: required build/entry files only.

There are no leave, approval, holiday, notification, calendar, balance-allocation or leave-type screens. Public registration is excluded: HR/ADMIN creates the employee profile, then its login user.

## Run and build

Use Node 22.12+ (or a newer Node supported by the locked Vite version). Run `npm ci`, then `npm run dev`. Start the backend separately on port 8080, or set `LMS_DEV_API` to its local address. The development server proxies `/api`.

Run `npm run build` for deployment. Upload the **contents of dist/** to the frontend S3 bucket. Use the CloudFront HTTPS output from the project stack, which routes `/api/*` to API Gateway stage1. HashRouter avoids S3 route-rewrite requirements.

Credentials remain in memory and are discarded on logout/reload. No AWS credentials, .env secrets, original node_modules or unrelated build configuration were copied.

Success messaging: ACCOUNT_CREATED and LOGIN_SUCCEEDED join employee CREATED/UPDATED/DELETED events. The app displays success messages; the outbox relay publishes safe metadata and a message to SNS/SQS. An optional SNS email subscription is configured through the NotificationEmail deployment parameter and requires recipient confirmation. An initial administrator login may have employeeId=null.

SNS uses protocol-specific messages: JSON for SQS, readable success notifications with a LeaveHub subject for confirmed email subscribers. Email addresses are deployment parameters, not source constants.
