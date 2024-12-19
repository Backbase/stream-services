export DOCKER_REGISTRY=<dockerRegistry>
export MAVEN_REPO=<targetRepositoryForMetaData>
export CRED_HELPER=<dockerRegisteryCredentialHelper>

export MVN_CMD = mvn package jib:build deploy  \
	-B -Ddocker.repository.url=$(DOCKER_REGISTRY) \
	-DaltDeploymentRepository=stream::default::$(MAVEN_REPO)

install:
	mvn clean install -DskipTest -Dmaven.test.skip=true

push-stream-cursor:
	$(MVN_CMD) -f stream-cursor/cursor-http/pom.xml
	$(MVN_CMD) -f stream-cursor/cursor-source/pom.xml

push-stream-job-profile-template:
	$(MVN_CMD) -f stream-job-profile-template/job-profile-template-http/pom.xml
	$(MVN_CMD) -f stream-job-profile-template/job-profile-template-task/pom.xml

push-stream-legal-entity:
	$(MVN_CMD) -f stream-legal-entity/legal-entity-bootstrap-task/pom.xml
	$(MVN_CMD) -f stream-legal-entity/legal-entity-http/pom.xml
	$(MVN_CMD) -f stream-legal-entity/legal-entity-sink/pom.xml
	$(MVN_CMD) -f stream-legal-entity/legal-entity-generator-source/pom.xml

push-stream-product-catalog:
	$(MVN_CMD) -f stream-product-catalog/product-catalog-http/pom.xml
	$(MVN_CMD) -f stream-product-catalog/product-catalog-task/pom.xml

push-stream-product:
	$(MVN_CMD) -f stream-product/product-sink/pom.xml

push-stream-transactions:
	$(MVN_CMD) -f stream-transactions/transactions-generator-processor/pom.xml
	$(MVN_CMD) -f stream-transactions/transactions-sink/pom.xml
	$(MVN_CMD) -f stream-transactions/transactions-http/pom.xml

push-stream-config-server:
	$(MVN_CMD) -f stream-config-server/pom.xml

all: install push-stream-cursor push-stream-job-profile-template push-stream-legal-entity push-stream-product push-stream-product-catalog push-stream-transactions
	echo "Finished"
