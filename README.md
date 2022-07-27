[![Build Stream Services](https://github.com/Backbase/stream-services/actions/workflows/build.yml/badge.svg)](https://github.com/Backbase/stream-services/actions/workflows/build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=com.backbase.stream%3Astream-services&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.backbase.stream%3Astream-services)
# Stream Services
Stream Services are an Open-Source accelerator to connect with Backbase "out-of-the-box" components that talk to DBS and are responsible for orchestrating calls to DBS. 
The orchestration of calling different services is written in a functional programming style on project reactor enabling resiliency and scalability. 
Stream Services are packaged as libraries that can be included in any project, REST Service and/or Spring Cloud Data Flow Applications.

The Services are listed with Service name and how they are packaged

Currently, the following DBS services are exposed as Stream Components:
* [Stream Legal Entity](stream-legal-entity/readme.md) (Lib, Rest, Task) → Legal Entity Ingestion Service that orchestrate all calls to DBS from a single aggregate model. This service is exposed as a library and a REST full service. Supports retry of the aggregate and uses the Legal Entity Ingestion Model to have a single interface into DBS. Requires Access Control, Product Summary
* [Stream Product Catalog](stream-cursor/readme.md) (Lib, Rest, Task) → Enabled bootstrapping of product types into DBS. Product Types are currently hardcoded in the streamTask definition. Orchestrates calls into Product Summary
* [Stream Access Control](stream-cursor/readme.md) (Lib) → The Stream Access Control library provides access to Access Control services from a single library and provides an easier abstraction layer to use these services. It mixes access to persistence and service api's to enable proper service to service comms for non DBS services such as Stream. Requires Access Control, Product Summary
* [Stream-Cursor](stream-cursor/readme.md) (Lib, Rest, Source)  → The Stream Cursor Source is listening to predefined DBS events such as On Login. For each login event, it will retrieve a list of products from entitlements and creates an Ingestion Cursor per product. Cursors can be stored in a RDBMS and published on a HTTP Web Socket or Spring Cloud Dataflow Source. Requires RDBMS, Access Control, Product Summary and Transactions. Login Event received from Authentication Starter or Identity (with Audit Events enabled)
* [Stream Transactions](stream-transactions) (Lib, Rest) → Allows ingestion into DBS in a controlled way with support for retry, rate limiting and configurable batch sizes. 
* [Stream DBS Clients](stream-dbs-clients/readme.md) -> The Stream DBS Clients are generated from Backbase OOTB specs and provide a reactive interface to interact with DBS services.
* [Stream Portfolio](stream-portfolio) (Lib) → Allows ingestion into DBS in a controlled way.

## Stream API Documentation

You can find listed here the API specification containing the opinionated model of the supported Stream Services

| Service                | OpenAPI Spec                                            | [Redoc](https://github.com/Redocly/redoc)                                                                         |
|------------------------|---------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| stream-legal-entity    | [openapi.yaml](api/stream-legal-entity/openapi.yaml)    | [Ingest Legal Entity API](https://engineering.backbase.com/stream-services/api/stream-legal-entity/index.html)    |
| stream-audit           | [openapi.yaml](api/stream-audit/openapi.yaml)           | [Audit](https://engineering.backbase.com/stream-services/api/stream-audit/index.html)                             |
| stream-approvals       | [openapi.yaml](api/stream-approvals/openapi.yaml)       | [Ingest Approval API](https://engineering.backbase.com/stream-services/api/stream-approvals/index.html)           |
| stream-product-catalog | [openapi.yaml](api/stream-product-catalog/openapi.yaml) | [Product Catalog API](https://engineering.backbase.com/stream-services/api/stream-product-catalog/index.html)     |
| stream-portfolio       | [openapi.yaml](api/stream-portfolio/openapi.yaml)    | [Portfolio Ingestion API](https://engineering.backbase.com/stream-services/api/stream-portfolio/index.html)    |

## Supported DBS versions

| DBS version        | Stream [version](https://github.com/Backbase/stream-services/releases) |
|--------------------|------------------------------------------------------------------------|
| 2022.04            | 2.71.0 to latest                                                       |
| 2021.09 (2.21.2.x) | 2.49.0 to 2.69.0                                                       |
| 2021.07 (2.21.0.x) | 2.44.0 to 2.48.0                                                       |
| 2.20.x             | 2.23.0 to 2.43.0                                                       |
| 2.19.x             | 2.1.0 to 2.22.0                                                        |

## Software License Terms
Please see the license terms here: https://github.com/Backbase/stream-services/blob/master/LICENSE.txt

## Contributing
You are welcome to provide bug fixes and new features in the form of pull requests. If you'd like to contribute, please be mindful of the following guidelines:

- All changes should be properly tested for common scenarios (i.e. if changing Legal Entity SAGA, test that your change doesn't affect in non-intended ways to LE ingestion and update).
- Try to avoid reformat of files that change the indentation, tabs to spaces etc., as this makes reviewing diffs much more difficult.
- Please make one change/feature per pull request.
- Use descriptive PR description and commit messages.
- Together with your changes, submit updated [CHANGELOG.md](CHANGELOG.md) in your PR using the next desired version as reference.
- After your pull request gets approved and integrated, GitHub actions will bump the `MINOR` version and deploy it to Backbase maven repository. *(e.g. 2.45.0 -> 2.46.0)*
    * For small fixes and patches utilize the `hotfix/` branch prefix, so once it is integrated the pipelines will automatically bump the `PATCH` version instead of the `MINOR`. *(e.g. 2.46.0 -> 2.46.1)*
