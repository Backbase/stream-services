## Stream Composition Services

- Module Path -> ./stream-services/stream-compositions
- Build all service-api, integration-api, cursors-api & events specifications -> mvn clean install

## Local Profile Server Ports Information

| Name               | Composition Server Port | Integration Server Port |
| ------------------ | ----------------------- | ----------------------- |
| Legal Entity       | 9001                    | 7001                    |
| Product Catalog    | 9002                    | 7002                    |
| Product            | 9003                    | 7003                    |
| Transaction        | 9004                    | 7004                    |
| Transaction Cursor | 9005                    | NA                      |

## Postman for composition chaining

- Stream Composition PostMan -> ./stream-services/stream-compositions/docs/postman/services/

  This postman has the below functionalities:

  - Streams 3.0 Chaining

    - Ingest Legal Entity Service  (Employee - Pull Mode) - This request will ingest an employee for Flow Case Manager.

      The User will be ingested in DBS & Identity with configured JobReferenceRole. The Integration API can be configured to assign relevant flow data groups for the necessary privileges.

      `"productChainEnabled" - The request level configuration will enable/disable the chaining to product & transaction composition. `

    - Ingest Legal Entity Service  (Corporate - Pull Mode) - This request will ingest a corporate/retail user.

      The sync/async configured can be controlled in the respective composition service application yml

  - Integration Ingestion Services
    These postman requests can be used for testing your Integration Services (Core Integration/3rd party Integrations)

  - Login as Corporate Customer

    This postman requests will help to validate the user authentication and authorization.

    - Example: If a corporate customer can be authenticated via Identity, be able to retrieve and assign the user context. This can be extended further to add your project/OOTB Integrations validation.

  - Login as Employee

    This postman requests will help to validate if the Flow CSR is authenticated and authorized.

    - Example: If CSR can login to case manager to view the onboarding cases, whether he is able to authenticate via Identity, able to retrieve profile and privileges necessary to view Tasks,etc.
