## Legal Entity Composition Service
* Module Path -> ./stream-services/stream-compositions/services/legal-entity-composition-service
* Build the service -> mvn clean install
* For Local environment set up, run the local profile -> mvn spring-boot:run -Dspring-boot.run.profiles=local

## Configuration Properties

| Property Path  | Property Description |
   | ------------- | ------------- |
backbase.stream.compositions.legal-entity.integration-base-url | The Integration base url, which pulls the data from core
backbase.stream.compositions.legal-entity.chains.product-composition.enabled | The toggle for chaining to be enabled/disabled
backbase.stream.compositions.legal-entity.chains.product-composition.base-url | The product composition service base url
backbase.stream.compositions.legal-entity.chains.product-composition.async | The toggle for composition chaining to be async or sync
backbase.stream.compositions.legal-entity.chains.events.enableCompleted | The toggle for enabling events on composition completion
backbase.stream.compositions.legal-entity.chains.events.enableFailed | The toggle for enabling events on composition failure
bootstrap.enabled | The toggle to bootstrap model bank Legal Entity, Job Reference Role & Administrator Users for local bootstrapping. This needs to be updated as per project requirements.


## Few Tips

* What should I do if I don't have Identity Integrated with the project? 
  
  In the application yml, disable the Identity Integration -> 
  backbase.stream.legalentity.sink.useIdentityIntegration: false

* What should I do if I don't want to Ingest the User Profile for the user?
  
  In the application yml, disable the User Profile ->
  * backbase.stream.legalentity.sink.userProfileEnabled: false
  * backbase.stream.legalentity.dbs.user-profile-manager-base-url -> Remove the base url
  