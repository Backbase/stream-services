## Transaction Composition Service

- Module Path -> ./stream-services/stream-compositions/services/transaction-composition-service
- Build the service -> mvn clean install
- For Local environment set up, run the local profile -> mvn spring-boot:run -Dspring-boot.run.profiles=local

## Configuration Properties

| Property Path                                                               | Property Description                                                  |
|-----------------------------------------------------------------------------|-----------------------------------------------------------------------|
| backbase.stream.compositions.transaction.defaultStartOffsetInDays           | The Transaction Cursor default start date                             |
| backbase.stream.compositions.transaction.integration-base-url               | The Integration base url, which pulls the data from core              |
| backbase.stream.compositions.transaction.events.enableCompleted             | The toggle for enabling events on composition completion              |
| backbase.stream.compositions.transaction.events.enableFailed                | The toggle for enabling events on composition failure                 |
| backbase.stream.compositions.transaction.cursor.enabled                     | The toggle for cursor to be enabled/disabled                          |
| backbase.stream.compositions.transaction.cursor.transactionIdsFilterEnabled | The toggle to enable/disable upserting transaction Id's to the Cursor |
| backbase.stream.compositions.transaction.cursor.base-url                    | The transaction cursor base url                                       |
