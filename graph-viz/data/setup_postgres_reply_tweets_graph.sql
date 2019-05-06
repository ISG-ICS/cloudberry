#SELECT pg_terminate_backend(pg_stat_activity.pid)
#FROM pg_stat_activity WHERE pg_stat_activity.datname =
#'graphtweet' AND pid <> pg_backend_pid();
#DROP DATABASE IF EXISTS graphtweet;

create user graphuser with password 'graphuser' createdb;


CREATE DATABASE graphtweet;

\c graphtweet



DROP TABLE IF EXISTS replytweets;

create table replytweets(
tweet_from bigint not null,
from_create_at timestamp not null,
from_longitude double precision,
from_latitude double precision,
tweet_to bigint not null,
to_create_at timestamp not null,
to_longitude double precision,
to_latitude double precision,
PRIMARY KEY (tweet_from));

\copy replytweets from 'replies.csv' (FORMAT CSV, DELIMITER(' '));

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO graphuser;
