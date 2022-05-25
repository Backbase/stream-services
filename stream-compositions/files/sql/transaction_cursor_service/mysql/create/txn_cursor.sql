CREATE TABLE `transaction_cursor_service`.`txn_cursor` (
  `id` VARCHAR(36) NOT NULL,
  `arrangement_id` VARCHAR(36) NOT NULL,
  `ext_arrangement_id` VARCHAR(50) NOT NULL,
  `last_txn_date` DATETIME NULL,
  `last_txn_ids` VARCHAR(2600) NULL,
  `legal_entity_id` VARCHAR(36) NOT NULL,
  `additions` LONGTEXT CHARACTER SET 'utf8' NULL,
  `status` VARCHAR(45) NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `arrangement_id_UNIQUE` (`arrangement_id` ASC))
ENGINE = InnoDB;