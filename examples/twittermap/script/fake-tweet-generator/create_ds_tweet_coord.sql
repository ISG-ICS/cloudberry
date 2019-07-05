use twitter;


drop dataset ds_tweet_coord if exists;

create dataset ds_tweet_coord (typeTweet) if not exists primary key id;

insert into ds_tweet_coord (
select value d
from ds_tweet d
<<<<<<< HEAD
where d.coordinate is not unknown or d.bounding_box is not unknown);
=======
where d.coordinate is not unknown or d.bounding_box is not unknown);
>>>>>>> 3cad8a79aea03bc2151972e34d9ecd430a06ce62
