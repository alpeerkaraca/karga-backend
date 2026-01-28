# Configure Debezium

In order to use application you need to configure your Debezium connectors.
The application has 4 different connectors.

In order to implement Inbox / Outbox and Saga Pattern these connectors should be running.

| Name                | Description                                   |
|---------------------|-----------------------------------------------|
| `auth-connector`    | Listens for auth-db WAL (Write Ahead Logs)    |
| `payment-connector` | Listens for payment-db WAL (Write Ahead Logs) |
| `trip-connector`    | Listens for trip-db WAL (Write Ahead Logs)    |
| `user-connector`    | Listens for user-db WAL (Write Ahead Logs)    |

## Configuring Debezium

- First you need to change placeholder values in `.json` files. For example {{auth_db_password}} -> SecurePass123!
- Send a POST request to `http://localhost:8083/connectors/` with json file as body.

```bash
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" \
  localhost:8083/connectors/ \
  -d @trip-outbox-connector.json
```

- To see saved connectors: `curl -X GET http://localhost:8083/connectors`
- To see status of a connector: curl -X GET `http://localhost:8083/connectors/<connector-name>/status`
