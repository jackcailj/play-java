# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table search_keyword (
  id                            bigint auto_increment not null,
  keyword                       varchar(255),
  constraint pk_search_keyword primary key (id)
);

create table search_media (
  id                            integer auto_increment not null,
  keyword                       varchar(255),
  media_id                      bigint,
  media_name                    varchar(255),
  author                        varchar(255),
  publisher                     varchar(255),
  score                         integer,
  constraint uq_search_media_keyword_media_id unique (keyword,media_id),
  constraint pk_search_media primary key (id)
);

create table search_record (
  record_id                     bigint auto_increment not null,
  create_date                   datetime(6),
  is_base                       tinyint(1) default 0,
  score                         double,
  constraint pk_search_record primary key (record_id)
);

create table search_record_detail (
  record_detail_id              bigint auto_increment not null,
  record_id                     bigint,
  search_media_id               integer,
  position                      integer,
  ndcg                          double,
  constraint uq_search_record_detail_search_media_id unique (search_media_id),
  constraint pk_search_record_detail primary key (record_detail_id)
);

create table search_score (
  id                            bigint auto_increment not null,
  record_id                     bigint,
  keyword                       varchar(255),
  dcg                           double,
  maxdcg                        double,
  zero_num                      bigint,
  one_num                       bigint,
  record_num                    bigint,
  constraint uq_search_score_record_id unique (record_id),
  constraint pk_search_score primary key (id)
);

alter table search_record_detail add constraint fk_search_record_detail_record_id foreign key (record_id) references search_record (record_id) on delete restrict on update restrict;
create index ix_search_record_detail_record_id on search_record_detail (record_id);

alter table search_record_detail add constraint fk_search_record_detail_search_media_id foreign key (search_media_id) references search_media (id) on delete restrict on update restrict;

alter table search_score add constraint fk_search_score_record_id foreign key (record_id) references search_record (record_id) on delete restrict on update restrict;


# --- !Downs

alter table search_record_detail drop foreign key fk_search_record_detail_record_id;
drop index ix_search_record_detail_record_id on search_record_detail;

alter table search_record_detail drop foreign key fk_search_record_detail_search_media_id;

alter table search_score drop foreign key fk_search_score_record_id;

drop table if exists search_keyword;

drop table if exists search_media;

drop table if exists search_record;

drop table if exists search_record_detail;

drop table if exists search_score;

