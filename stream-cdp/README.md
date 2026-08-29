# Stream Audiences Integration
The goal of this module is to enable ingestion of Customers into Retail Customers and Business Customers segments.

The ingestion is done through an HTTP call towards `User Segments Collector` service.

`UserKindSegmentationSaga` (responsible for triggering the ingestion towards the collector) is triggered from `LegalEntitySaga`