## Product Composition Service
* Module Path -> ./stream-services/stream-compositions/services/product-composition-service
* Build the service -> mvn clean install
* For Local environment set up, run the local profile -> mvn spring-boot:run -Dspring-boot.run.profiles=local

## Configuration Properties

| Property Path  | Property Description |
   | ------------- | ------------- |
backbase.stream.compositions.product.integration-base-url | The Integration base url, which pulls the data from core
backbase.stream.compositions.product.chains.transaction-composition.enabled | The toggle for chaining to be enabled/disabled
backbase.stream.compositions.product.chains.transaction-composition.base-url | The transaction composition service base url
backbase.stream.compositions.product.chains.transaction-composition.async | The toggle for composition chaining to be async or sync
backbase.stream.compositions.product.chains.transaction-composition.excludeProductTypeExternalIds | The Product Types to be excluded during chaining
backbase.stream.compositions.product.chains.events.enableCompleted | The toggle for enabling events on composition completion
backbase.stream.compositions.product.chains.events.enableFailed | The toggle for enabling events on composition failure
backbase.stream.compositions.product.ingestion-mode.function-groups | The Ingestion mode for function groups [UPSERT,REPLACE]
backbase.stream.compositions.product.ingestion-mode.data-groups | The Ingestion mode for data groups [UPSERT,REPLACE]
backbase.stream.compositions.product.ingestion-mode.arrangements | The Ingestion mode for arrangments [UPSERT,REPLACE]
