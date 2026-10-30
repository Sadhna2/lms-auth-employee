Public Amazon RDS CA truststore for ap-south-1, downloaded from:
https://truststore.pki.rds.amazonaws.com/ap-south-1/ap-south-1-bundle.pem

Contains trusted public certificates only; there are no private keys or account credentials.
The public-ca-only store password is not an AWS/database/application credential.
Only JDBC uses this store through trustCertificateKeyStoreUrl. The SNS SDK retains its
normal public HTTPS trust store. VERIFY_IDENTITY remains enabled.
Refresh CA certificates before AWS certificate rotation requires it.
