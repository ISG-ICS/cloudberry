import pymysql
import json

# Connect to the database
connection = pymysql.connect(host='localhost',
                             user='josephzheng',
                             password='981118Ztc!',
                             db='moviedb',
                             charset='utf8mb4',
                             cursorclass=pymysql.cursors.SSDictCursor)

try:

    with connection.cursor() as cursor:
        sql = "SELECT * FROM stars LIMIT 1000"
        cursor.execute(sql)
        result = cursor.fetchall_unbuffered()
        with open('stars.txt', 'w') as json_file:
            json.dump(list(result), json_file)

        sql = "SELECT * FROM movies LIMIT 1000"
        cursor.execute(sql)
        result = cursor.fetchall_unbuffered()
        with open('movies.txt', 'w') as json_file:
            json.dump(list(result), json_file)
finally:
    connection.close()
