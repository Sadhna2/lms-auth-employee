# LMS success notifications

The app shows success messages immediately. Explicit successful login records LOGIN_SUCCEEDED; HR/ADMIN login-user registration records ACCOUNT_CREATED. Employee profile changes record CREATED, UPDATED or DELETED.

Changes are stored in the MySQL outbox transaction. The Java 17 relay publishes committed events on its next scheduled run, normally within about one minute. SNS delivers structured JSON to SQS and readable project notifications to the confirmed NotificationEmail subscriber. The email has a LeaveHub subject. Confirm the AWS SNS subscription email before testing email delivery.

Messages contain an event ID, optional employee ID, operation and success message. They contain no email address, password, hash, home address or phone number. The initial administrator can have employeeId=null. Delivery is at least once; consumers should deduplicate by eventId. Basic authentication on ordinary API requests does not generate another login event; the explicit login endpoint does.

No public subscription/publish controller, hardcoded AWS credentials, or demo topic/queue names were copied. AWS SDK credentials come from the Lambda execution role. The email recipient is a protected deployment parameter, not a source constant.
