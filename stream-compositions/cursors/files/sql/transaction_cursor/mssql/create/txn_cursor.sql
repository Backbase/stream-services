CREATE TABLE [dbo].[txn_cursor]
(
    [id] [nvarchar](36) NOT NULL,
    [arrangement_id] [nvarchar](36) NULL,
    [ext_arrangement_id] [nvarchar](50) NULL,
    [last_txn_date] [datetime] NULL,
    [last_txn_ids] [nvarchar](4000) NULL,
    [legal_entity_id] [nvarchar](36) NULL,
    [additions] [nvarchar](max) NULL,
    [status] [nvarchar](45) NULL,
    CONSTRAINT [pk_txn_cursor] PRIMARY KEY CLUSTERED (
        [id] ASC
    ),
    CONSTRAINT [uq_ext_arrangement_id] UNIQUE NONCLUSTERED
    (
        [ext_arrangement_id] ASC
    )
);
