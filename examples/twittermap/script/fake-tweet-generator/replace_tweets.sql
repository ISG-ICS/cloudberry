use twitter;


drop dataset ds_tweet_coord_user if exists;

create dataset ds_tweet_coord_user (typeUser) if not exists primary key id;

insert into ds_tweet_coord_user(
select value (select value g.d from g limit 1)
from (select distinct value user from ds_tweet_coord) d
group by d.id
group as g);


drop dataset ds_tweet_coord_user_replaced if exists;

create dataset ds_tweet_coord_user_replaced (typeUser) if not exists primary key id;

insert into ds_tweet_coord_user_replaced(
select d.*, f.name, f.screen_name
from ds_tweet_coord_user d
join fname f
on d.id  = f.id);


drop dataset ds_tweet_coord_text_replaced if exists;

create dataset ds_tweet_coord_text_replaced (typeTweet) if not exists primary key id;

insert into ds_tweet_coord_text_replaced(
select d.*, f.text
from ds_tweet_coord d
join ftext f
on d.id  = f.id);


drop dataset ds_tweet_coord_replaced if exists;

create dataset ds_tweet_coord_replaced (typeTweet) primary key id;

insert into ds_tweet_coord_replaced (
select d.*, f as user
from ds_tweet_coord_text_replaced d
join ds_tweet_coord_user_replaced f
on d.user.id  = f.id);