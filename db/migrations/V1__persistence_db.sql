CREATE TABLE IF NOT EXISTS journal
(
    ordering        BIGSERIAL,
    persistence_id  VARCHAR(255) NOT NULL,
    sequence_number BIGINT       NOT NULL,
    deleted         BOOLEAN      DEFAULT FALSE,
    tags            VARCHAR(255) DEFAULT NULL,
    message         BYTEA        NOT NULL,
    PRIMARY KEY (persistence_id, sequence_number)
);

CREATE UNIQUE INDEX IF NOT EXISTS journal_ordering_idx ON journal (ordering);

CREATE TABLE IF NOT EXISTS snapshot
(
    persistence_id  VARCHAR(255) NOT NULL,
    sequence_number BIGINT       NOT NULL,
    created         BIGINT       NOT NULL,
    snapshot        BYTEA        NOT NULL,
    PRIMARY KEY (persistence_id, sequence_number)
);


CREATE TABLE IF NOT EXISTS read_side_offsets
(
    read_side_id     VARCHAR(255),
    tag              VARCHAR(255),
    sequence_offset  bigint,
    time_uuid_offset char(36),
    PRIMARY KEY (read_side_id, tag)
);

create table if not exists "AKKA_PROJECTION_OFFSET_STORE" (
  "PROJECTION_NAME" VARCHAR(255) NOT NULL,
  "PROJECTION_KEY" VARCHAR(255) NOT NULL,
  "CURRENT_OFFSET" VARCHAR(255) NOT NULL,
  "MANIFEST" VARCHAR(4) NOT NULL,
  "MERGEABLE" BOOLEAN NOT NULL,
  "LAST_UPDATED" BIGINT NOT NULL
);

create index "PROJECTION_NAME_INDEX" on "AKKA_PROJECTION_OFFSET_STORE" ("PROJECTION_NAME");

alter table "AKKA_PROJECTION_OFFSET_STORE"
  add constraint "PK_PROJECTION_ID" primary key("PROJECTION_NAME","PROJECTION_KEY");
