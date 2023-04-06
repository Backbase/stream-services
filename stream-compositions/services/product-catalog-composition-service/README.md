## Product Catalog Composition Service

- Module Path -> ./stream-services/stream-compositions/services/product-catalog-composition-service
- Build the service -> mvn clean install
- For Local environment set up, run the local profile -> mvn spring-boot:run -Dspring-boot.run.profiles=local

## Configuration Properties

| Property Path                                                                | Property Description                                     |
|------------------------------------------------------------------------------|----------------------------------------------------------|
| backbase.stream.compositions.product-catalog.product-catalog-integration-url | The Integration base url, which pulls the data from core |
| backbase.stream.compositions.product-catalog.enable-completed-events         | The toggle for enabling events on catalog completion     |
| backbase.stream.compositions.product-catalog.enable-failed-events            | The toggle for enabling events on catalog failure        |
