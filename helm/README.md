# Helm Charts

All helm charts available for deploying stream workloads in a Kubernetes cluster.

## [stream-bootstrap-task](stream-bootstrap-task)

Helm chart used to create a job to execute a vanilla Stream boostrap task.

## Usage

Utilization example using [helmfile](https://helmfile.readthedocs.io/en/latest/):

```yaml
releases:
  - name: stream-legal-entity-boostrap
    chart: oci://repo.backbase.com/backbase-stream-images/stream-bootstrap-task
    version: 1.0.0
    labels:
      tier: job
      component: bootstrap-task
    values:
      - image:
          repository: legal-entity-bootstrap-task
          tag: 3.8.0
          pullPolicy: IfNotPresent
        dependencies:
          - serviceName: token-converter
          - serviceName: access-control
          - serviceName: user-manager
          - serviceName: arrangement-manager
          - serviceName: identity-integration-service
          - serviceName: backbase-identity
            path: /auth/realms/backbase/.well-known/openid-configuration
            healthIndicator: backbase
        initContainers:
          - name: init-product-catalog
            image: repo.backbase.com/backbase-stream-images/product-catalog-task:3.8.0
            env:
              - name: SPRING_PROFILES_INCLUDE
                value: moustache-bank
        env:
          SPRING_PROFILES_INCLUDE: moustache-bank,moustache-bank-subsidiaries
```

## Deployment

The deployment of these charts are not yet automated, hence to make a new version available you have to go through the following steps:

```shell
helm package stream-bootstrap-task
helm registry login repo.backbase.com
helm push stream-bootstrap-task-<NEW VERSION>.tgz oci://repo.backbase.com/backbase-stream-images
```
