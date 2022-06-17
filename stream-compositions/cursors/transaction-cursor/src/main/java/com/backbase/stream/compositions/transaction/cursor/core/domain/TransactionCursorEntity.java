package com.backbase.stream.compositions.transaction.cursor.core.domain;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import org.hibernate.annotations.GenericGenerator;

/**
 * The Domain Model for the transaction cursor service
 */
@Entity
@Table(name = "txn_cursor")
public class TransactionCursorEntity {

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getArrangementId() {
    return arrangementId;
  }

  public void setArrangementId(String arrangementId) {
    this.arrangementId = arrangementId;
  }

  public String getExtArrangementId() {
    return extArrangementId;
  }

  public void setExtArrangementId(String extArrangementId) {
    this.extArrangementId = extArrangementId;
  }

  public Timestamp getLastTxnDate() {
    return lastTxnDate;
  }

  public void setLastTxnDate(Timestamp lastTxnDate) {
    this.lastTxnDate = lastTxnDate;
  }

  public String getLastTxnIds() {
    return lastTxnIds;
  }

  public void setLastTxnIds(String lastTxnIds) {
    this.lastTxnIds = lastTxnIds;
  }

  public String getLegalEntityId() {
    return legalEntityId;
  }

  public void setLegalEntityId(String legalEntityId) {
    this.legalEntityId = legalEntityId;
  }

  public String getAdditions() {
    return additions;
  }

  public void setAdditions(String additions) {
    this.additions = additions;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @Id
  @GeneratedValue(
      generator = "system-uuid"
  )
  @GenericGenerator(
      name = "system-uuid",
      strategy = "uuid2"
  )
  @Column(
      name = "id",
      updatable = false,
      nullable = false,
      length = 36
  )
  private String id;

  @Column(
      name = "arrangement_id",
      updatable = false,
      nullable = false,
      length = 36
  )
  private String arrangementId;

  @Column(
      name = "ext_arrangement_id",
      nullable = false,
      length = 50
  )
  private String extArrangementId;


  @Column(
      name = "last_txn_date",
      nullable = false
  )
  private Timestamp lastTxnDate;

  @Column(
      name = "last_txn_ids",
      nullable = false,
      length = 2600
  )
  private String lastTxnIds;

  @Column(
      name = "legal_entity_id",
      nullable = false,
      length = 36
  )
  private String legalEntityId;

  @Column(
      name = "additions",
      nullable = false
  )
  private String additions;

  @Column(
      name = "status",
      nullable = false,
      length = 45
  )
  private String status;

}
