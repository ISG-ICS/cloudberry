use twitter;


drop dataset ds_tweet_coord if exists;

create dataset ds_tweet_coord (typeTweet) if not exists primary key id;

insert into ds_tweet_coord (
select value d
from ds_tweet d
where d.coordinate is not unknown or d.bounding_box is not unknown);
