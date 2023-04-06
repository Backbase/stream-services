# End-to-End Tests

In this folder we will maintain automated tests to validate the end-to-end functionality of the Stream capabilities.

> As of this version (v3.9.2) we haven't automated this test yet. Manual execution is needed.

## Docker Compose

This docker compose file will spin up a minimal multi-tenant setup to test the following Stream Components:

- legal-entity-bootstrap-task (Tenant 1)
- legal-entity-composition-service (Tenant 2)

The boostrap execution will be invoked in each component listed above and the validation should be to check weather it
was successfully executed or not.

### Execution

```shell
docker compose up -d
```

### Validation

```shell
docker compose ps legal-entity-bootstrap-task
docker compose ps legal-entity-composition-service
```

## Future Improvements

- Automate execution using TestContainers;
- Implement rest api checks to validate the data content created.
