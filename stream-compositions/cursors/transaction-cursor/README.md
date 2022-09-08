
## Transaction Cursor
* Module Path -> ./stream-services/stream-compositions/cursors/transaction-cursor
  * Build the service -> mvn clean install
  * For Local environment set up, run the local profile -> mvn spring-boot:run -Dspring-boot.run.profiles=local

## Setup Liquibase
### Create Liquibase configuration files
  * Create [src/main/resources/db.changelog/db.changelog-1.0.0.xml ](src/main/resources/db/changelog/db.changelog-1.0.0.xml) 
    * This file has the instructions to create/modify the database. Refer the [Liquibase Quickstart Guide](https://www.liquibase.org/get-started/quickstart) on how to create changeLog file.
  * Create [src/main/resources/db.changelog/db.changelog-persistence.xml ](src/main/resources/db/changelog/db.changelog-persistence.xml) 
    * This file is to maintain all the liquibase changeLog files in one place and can be referred as a single entry point to create/modify database using the property `liquibase.change-log`. 
### Add Liquibase dependencies
  * Add below dependency to pom.xml -   
    `<dependency>`  
  &nbsp;&nbsp;&nbsp;&nbsp;`<groupId>org.liquibase</groupId>`  
  &nbsp;&nbsp;&nbsp;&nbsp;`<artifactId>liquibase-core</artifactId>`  
  &nbsp;&nbsp;&nbsp;&nbsp;`<version>4.9.1</version>`  
    `</dependency>`
### Configure Liquibase properties
  * Add below properties to src/main/resources/application.yml -    
    `liquibase:`  
  &nbsp;&nbsp;&nbsp;&nbsp;` enabled: true`  
  &nbsp;&nbsp;&nbsp;&nbsp;` change-log: db/changelog/db.changelog-persistence.xml`
### Configure Spring JDBC properties
  * Add below properties to src/main/resources/application.yml -  
    `spring:`  
    &nbsp;&nbsp;&nbsp;&nbsp;`jpa:`  
    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`database-platform: <hibernate_dialect>`  
    &nbsp;&nbsp;&nbsp;&nbsp;`datasource:`  
    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`username: <db_username>`  
    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`password: <db_password>`  
    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`url: <db_url>`  
    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`driver-class-name: <db_driver_class>`

**Note: When the command `mvn spring-boot:run` is executed, liquibase will create/modify the database as per the configuration.** 