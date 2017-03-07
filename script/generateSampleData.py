import random

text1 = '{ "create_at": datetime("2015-11-23T16:14:03.000Z"), "id": 668945640186101761, "text": "Just posted a photo @ Campus Martius Park https://t.co/5Ax4E2CdWZ", "in_reply_to_status": -1, "in_reply_to_user": -1, "favorite_count": 0, "coordinate": point("-83.04647491,42.33170228"), "retweet_count": 0, "lang": "en", "is_retweet": false, "user": { "id": 48121888, "name": "Kevin McKague", "screen_name": "KevinOfMI", "lang": "en", "location": "Davison, Michigan", "create_at": date("2009-06-17"), "description": "I need to ride my bike until my brain shuts up and my muscles are screaming. \\nRight after these donuts. Dad of 3.\\n Visit my blog 18 Wheels and a 12-Speed Bike.", "followers_count": 1178, "friends_count": 1780, "statues_count": 22263 }, "place": { "country": "United States", "country_code": "United States", "full_name": "Detroit, MI", "id": "b463d3bd6064861b", "name": "Detroit", "place_type": "city", "bounding_box": rectangle("'
middleText = '-83.288056,42.255085 -82.91052,42.450488'
text2 = '") }, "geo_tag": { "stateID": 26, "stateName": "Michigan", "countyID": 26163, "countyName": "Wayne", "cityID": 2622000, "cityName": "Detroit" } }'

count = 200

for i in range(count):
    sign = random.randrange(0, 3, step=2) - 1
    n = random.randrange(0,1000000) * sign * 1.0 / 1000000
    num1 = [-83.288056, 42.255085]
    num2 = [-82.91052, 42.450488]
    # print "n: " + str(n)
    num1[0] += n
    num1[1] += n
    num2[0] += n
    num2[1] += n
    middleText = "" + str(num1[0]) + "," + str(num1[1]) + " " + str(num2[0]) + "," + str(num2[1])
    # print "middleText: " + middleText
    print text1 + middleText + text2

