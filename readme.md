# Stream Services
Stream Services are the "out-of-the-box" components that talk to DBS are responsible for orchestrating calls to DBS. 
The orchestration of calling different services is written in a functional programming style on project reactor enabling resiliency and scalability. 
Stream Services are packaged as libraries that can be included in any project, REST Service and/or Spring Cloud Data Flow Applications

The Services are listed with Service name and how they are packaged

Currently the following DBS services are exposed as Stream Components:
* [Stream DBS Clients](stream-dbs-clients/readme.md) -> The Stream DBS Clients are generated from the RAML spec and provide a reactive interface in using them.
* [Stream-Cursor](stream-cursor/readme.md) (Lib, Rest, Source)  → The Stream Cursor Source is listening to predefined DBS events such as On Login. For each login event, it will retrieve a list of products from entitlements and creates an Ingestion Cursor per product. Cursors can be stored in a RDBMS and published on a HTTP Web Socket or Spring Cloud Dataflow Source. Requires RDBMS, Access Control, Product Summary and Transactions. Login Event received from Authentication Starter or Identity (with Audit Events enabled)
* [Stream Access Control](stream-cursor/readme.md) (Lib) → The Stream Access Control library provides access to Access Control services from a single library and provides an easier abstraction layer to use these services. It mixes access to persistence and service api's to enable proper service to service comms for non DBS services such as Stream. Requires Access Control, Product Summary
* [Stream Job Profile](stream-cursor/readme.md) Template (Lib, Rest, Task)  → Stream Job Profile Templates manages templates for assigning business functions to newly created users during the ingestion process. Templates are loaded from a CSV file by the Setup Job Profile Task. Requires RDBMS and Product Summary.  NOTE: This functionality is expected to offered out of the box in 2020 but I couldn't wait.
* [Stream Legal Entity](stream-cursor/readme.md) (Lib, Rest, Sink) → Legal Entity Ingestion Service that orchestrate all calls to DBS from a single aggregate model. This service is exposed as a library, REST full service and as a Sink. Supports retry of the aggregate and uses the Legal Entity Ingestion Model to have a single interface into DBS. Requires Access Control, Product Summary
* [Stream Product Catalog](stream-cursor/readme.md) (Lib, Rest, Task) → Enabled bootstrapping of product types into DBS. Product Types are currently hardcoded in the streamTask definition. Orchestrates calls into Product Summary
* [Stream Transactions](stream-cursor/readme.md) (Lib, Rest, Sink) → Allows ingestion into DBS in a controlled way with support for retry, rate limiting and configurable batch sizes. 

## Stream Configuration Server
To allow an easy configuration of Stream Services you can use the special Stream Config Service which is based on Spring Config Server.

More information on Stream Config Server can be fou§nd in the [stream-config-server/readme.md](stream-config-server/readme.md)

## Contributing
Want to contribute to the code? Please take a moment to read our [Contributing](CONTRIBUTING.md) guide to learn about our development process.

