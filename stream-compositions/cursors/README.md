## Cursors

* Create the **transaction_cursor** database & **txn_cursor** table
    * Module Path -> ./stream-services/stream-compositions/cursors
    * mvn clean install -Pclean-database

## Cursors Database Scripts

* The scripts are located at this path  ./stream-services/stream-compositions/cursors/files/sql/transaction_cursor
  * MSSQL -> ./stream-services/stream-compositions/cursors/files/sql/transaction_cursor/mssql/create
  * MYSQL -> ./stream-services/stream-compositions/cursors/files/sql/transaction_cursor/mysql/create
  * ORACLE -> ./stream-services/stream-compositions/cursors/files/sql/transaction_cursor/oracle/create
    

## Transaction Cursor
The transaction cursor ingests the cursor to track the last ingested transaction id's with respective statuses, which helps transaction integration service to retrieve the delta in the next iterations for the arrangmentId.

* Transaction Cursor PostMan -> ./stream-services/stream-compositions/docs/postman/cursors/
  
`Please update the http port as per your project set up`

This Postman has requests for the below functionalities:
  * Get Transaction Cursor by ArrangementId
  * Get Transaction Cursor by ID
  * Delete Transaction Cursor by ArrangementId
  * Upsert Transaction Cursor (Save)
  * Upsert Transaction Cursor (Update)
  * Patch Transaction Cursor by ArrangementId
  * Filter Transaction Cursor
