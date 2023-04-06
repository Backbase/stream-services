CREATE TABLE txn_cursor (
  id    VARCHAR2(36) NOT NULL,
  arrangement_id    VARCHAR2(36) NOT NULL,
  ext_arrangement_id    VARCHAR2(50) NOT NULL,
  last_txn_date TIMESTAMP NOT NULL,
  last_txn_ids  VARCHAR2(4000) NOT NULL,
  legal_entity_id   VARCHAR2(36) NOT NULL,
  additions NCLOB NULL,
  status    VARCHAR2(45) NULL,
  CONSTRAINT pk_txn_cursor  PRIMARY KEY (id),
  CONSTRAINT uq_arrangement_id UNIQUE (arrangement_id)
);
