# Legal Entity Ingestion SAGA

The Legal Entity API can be used to quickly onboard Customers into Backbase DBS by orchestrating the required REST calls on the Backbase DBS Services.

The API uses a special Ingestion Model which is an aggregate of all objects necessary to create a Legal Entity and their supporting objects:

* Legal Entity
* Users able to login on behalf of this Legal Entity
* Service Agreement setup between the users and the Legal Entity
* Job Profile Templates containing Business Functions, Privileges and Limits to be applied between user and service agreement
* Grouped Arrangements with Job Profile Templates for assigned users

## How it works

The Legal Entity Saga orchestrates calls to Backbase DBS using information pass in the Legal Entity Ingestion Model. 

![Sequence Diagram](docs/legal_enttiy_saga_sequence.png)

Communication to DBS is handled through the generated clients from `stream-dbs-cleints`


## Usage

The Legal Entity Saga can be used in several ways:

* **HTTP REST** - Adhoc processing of Legal Entities
* **Spring Cloud Data Flow Task** - Bootstrap Legal Entity Hierarchy from configuration
* **Spring Cloud Data Flow Sink** - Spring Cloud Stream Input Channel for continuously processing Legal Entities
* **Library** - Java Library used in All-in-one services 

## Service Configuration

The Legal Entity Saga uses the **ServiceAPI** and therefore **must** communicate with presentation services directly.
 
Regardless of which usage type, the Legal Entity Saga must be supplied with the following configuration:

| Property        | Value | Description  |
| ------------- |-------------| -----|
| `spring.security.oauth2.client.provider.dbs.token-uri`                        | https://TOKENCONVERTOR/api/token-converter/oauth/token    | The internal URL of the Platform Token Converter Service   |
| `spring.security.oauth2.client.registration.dbs.authorization-grant-type`     | client_credentials                                        | OAuth Flow  |
| `spring.security.oauth2.client.registration.dbs.client-id`                    | bb-client                                                 | OAuth Client ID |
| `spring.security.oauth2.client.registration.dbs.client-secret`                | bb-secret                                                 | OAuth Client Secret |
| `spring.security.oauth2.client.registration.dbs.client-authentication-method` | post                                                      | OAuth Authentication Method |
| `backbase.stream.dbs.access-control-pandp-base-url`                           | https://ACCESSCONTROLPANDPSERVICE/service-api/v2          | Internal URL to Access Control PandP Service |
| `backbase.stream.dbs.access-group-presentation-base-url`                      | https://ACCESSGROUPPRESENTATIONSERVICE/service-api/v2     | Internal URL to Access Group Presentation Service |
| `backbase.stream.dbs.account-presentation-base-url`                           | https://ACCOUNTPRESENTATIONSERVICE/service-api/v2         | Internal URL to Account Presentation Service |
| `backbase.stream.dbs.legal-entity-presentation-base-url`                      | https://LEGALENTITYPRESENTATIONSERVICE/service-api/v2     | Internal URL to Legal Entity Presentation Service
| `backbase.stream.dbs.user-presentation-base-url`                              | https://USERPRESENTATIONSERVICE/service-api/v2            | Internal URL to User Presentation Service |
| `backbase.stream.dbs.transaction-presentation-base-url`                       | https://TRANSACTIONPRESENTATIONSERVICE/service-api/v2     | Internal URL to Transaction Presentation Service |

### Example Configurations
The following configuration is useful for testing and development where the presentation services have public ingress:

```yaml
spring:
  security:
    oauth2:
      client:
        provider:
          dbs:
            token-uri: https://stream-demo.proto.backbasecloud.com/api/token-converter/oauth/token
        registration:
          dbs:
            authorization-grant-type: client_credentials
            client-id: bb-client
            client-secret: bb-secret
            client-authentication-method: post

backbase:
  stream:
    dbs:
      access-control-pandp-base-url: https://stream-api.proto.backbasecloud.com/accesscontrol-pandp-service/service-api/v2
      access-group-presentation-base-url: https://stream-api.proto.backbasecloud.com/accessgroup-presentation-service/service-api/v2
      account-presentation-base-url: https://stream-api.proto.backbasecloud.com/account-presentation-service/service-api/v2
      legal-entity-presentation-base-url:  https://stream-api.proto.backbasecloud.com/legalentity-presentation-service/service-api/v2
      user-presentation-base-url: https://stream-api.proto.backbasecloud.com/user-presentation-service/service-api/v2
      transaction-presentation-base-url: https://stream-api.proto.backbasecloud.com/transaction-presentation-service/service-api/v2
```

When deploying in a Kubernetes environment the configuration should look something like this:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          dbs:
            authorization-grant-type: client_credentials
            client-id: bb-client
            client-secret: bb-secret
            client-authentication-method: post
        provider:
          dbs:
            token-uri: https://stream-demo.proto.backbasecloud.com/api/token-converter/oauth/token
backbase:
  stream:
    dbs:
      access-group-presentation-base-url: http://dbs-accessgrouppresentationservice.stream-demo.svc.cluster.local:8080/accessgroup-presentation-service/service-api/v2
      access-control-pandp-base-url: http://dbs-accesscontrolpandpservice.stream-demo.svc.cluster.local:8080/accesscontrol-pandp-service/service-api/v2
      account-presentation-base-url: http://dbs-accountpresentationservice.stream-demo.svc.cluster.local:8080/account-presentation-service/service-api/v2
      legal-entity-presentation-base-url:  http://dbs-legalentitypresentationservice.stream-demo.svc.cluster.local:8080/legalentity-presentation-service/service-api/v2
      user-presentation-base-url: http://dbs-userpresentationservice.stream-demo.svc.cluster.local:8080/user-presentation-service/service-api/v2
      transaction-presentation-base-url: http://dbs-transactionpresentationservice.stream-demo.svc.cluster.local:8080/transaction-presentation-service/service-api/v2
```

## Legal Entity Bootstrap

For the initial ingestion of the Legal Entity, you can use the Spring Boot Task `legal-entity-bootstrap-task`. 

Examples and usage instructions can be found in the [readme.md](legal-entity-bootstrap-task/readme.md).


## Legal Entity HTTP

For processes that require an HTTP endpoint for ingestion of Legal Entities, the Legal Entity HTTP Service be deployed. 
The Legal Entity HTTP Service can be used to ingest Legal Entities both synchronously and asynchronously.

Examples and usage instructions can be found in the [readme.md](legal-entity-http/readme.md).


## Legal Entity Sink

The Legal Entity Sink can be used in Spring Cloud Data Flow and serve as a Sink to Ingest Legal Entities. 

Examples and usage instructions can be found in the [readme.md](legal-entity-sink/readme.md). 