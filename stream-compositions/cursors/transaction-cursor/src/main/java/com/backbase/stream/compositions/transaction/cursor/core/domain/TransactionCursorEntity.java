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

  public String getArrangement_id() {
    return arrangement_id;
  }

  public void setArrangement_id(String arrangement_id) {
    this.arrangement_id = arrangement_id;
  }

  public String getExt_arrangement_id() {
    return ext_arrangement_id;
  }

  public void setExt_arrangement_id(String ext_arrangement_id) {
    this.ext_arrangement_id = ext_arrangement_id;
  }

  public Timestamp getLast_txn_date() {
    return last_txn_date;
  }

  public void setLast_txn_date(Timestamp last_txn_date) {
    this.last_txn_date = last_txn_date;
  }

  public String getLast_txn_ids() {
    return last_txn_ids;
  }

  public void setLast_txn_ids(String last_txn_ids) {
    this.last_txn_ids = last_txn_ids;
  }

  public String getLegal_entity_id() {
    return legal_entity_id;
  }

  public void setLegal_entity_id(String legal_entity_id) {
    this.legal_entity_id = legal_entity_id;
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
  private String arrangement_id;

  @Column(
      name = "ext_arrangement_id",
      nullable = false,
      length = 50
  )
  private String ext_arrangement_id;


  @Column(
      name = "last_txn_date",
      nullable = false
  )
  private Timestamp last_txn_date;

  @Column(
      name = "last_txn_ids",
      nullable = false,
      length = 2600
  )
  private String last_txn_ids;

  @Column(
      name = "legal_entity_id",
      nullable = false,
      length = 36
  )
  private String legal_entity_id;

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
