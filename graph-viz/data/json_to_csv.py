import json
import csv

# for float comparison
eps = 1e-5

# status = 1, 2, 3, 4
status = 1


def detail_error_detect(data, label, i):
    try:
        _ = data[i]['ds_tweet_reply_graph'][label]
        return True
    except KeyError:
        return False


def key_error_detect(data, i):
    global status
    has_from_coordinate = detail_error_detect(data, 'from_coordinate', i)
    has_to_coordinate = detail_error_detect(data, 'to_coordinate', i)
    if has_from_coordinate and has_to_coordinate:
        status = 1
    elif has_from_coordinate and not has_to_coordinate:
        status = 2
    elif not has_from_coordinate and has_to_coordinate:
        status = 3
    else:
        status = 4


def create_list_from_json(json_file):
    with open(json_file) as f:
        data = json.load(f)

    all_data = []
    for i in range(len(data)):
        key_error_detect(data, i)
        x_from_box = (data[i]['ds_tweet_reply_graph']['from_bounding_box'][0][0] +
                      data[i]['ds_tweet_reply_graph']['from_bounding_box'][1][0]) / 2
        x_to_box = (data[i]['ds_tweet_reply_graph']['to_bounding_box'][0][0] +
                    data[i]['ds_tweet_reply_graph']['to_bounding_box'][1][0]) / 2
        y_from_box = (data[i]['ds_tweet_reply_graph']['from_bounding_box'][0][1] +
                      data[i]['ds_tweet_reply_graph']['from_bounding_box'][1][1]) / 2
        y_to_box = (data[i]['ds_tweet_reply_graph']['to_bounding_box'][0][1] +
                    data[i]['ds_tweet_reply_graph']['to_bounding_box'][1][1]) / 2
        source = []
        target = []
        # if source and target are the same, don't add the record
        if status == 1:
            source = [data[i]['ds_tweet_reply_graph']['from_coordinate'][0],
                      data[i]['ds_tweet_reply_graph']['from_coordinate'][1]]
            target = [data[i]['ds_tweet_reply_graph']['to_coordinate'][0],
                      data[i]['ds_tweet_reply_graph']['to_coordinate'][1]]
        elif status == 2:
            source = [data[i]['ds_tweet_reply_graph']['from_coordinate'][0],
                      data[i]['ds_tweet_reply_graph']['from_coordinate'][1]]
            target = [x_to_box, y_to_box]
        elif status == 3:
            source = [x_from_box, y_from_box]
            target = [data[i]['ds_tweet_reply_graph']['to_coordinate'][0],
                      data[i]['ds_tweet_reply_graph']['to_coordinate'][1]]
        elif status == 4:
            source = [x_from_box, y_from_box]
            target = [x_to_box, y_to_box]
        if abs(source[0] - target[0]) <= eps \
                and abs(source[1] - target[1]) <= eps:
            continue

        # ignore "\n" and "|" in texts
        data[i]['ds_tweet_reply_graph']['from_text'] = data[i]['ds_tweet_reply_graph']['from_text'].replace("\n", " ")
        data[i]['ds_tweet_reply_graph']['from_text'] = data[i]['ds_tweet_reply_graph']['from_text'].replace("|", "")
        data[i]['ds_tweet_reply_graph']['to_text'] = data[i]['ds_tweet_reply_graph']['to_text'].replace("\n", " ")
        data[i]['ds_tweet_reply_graph']['to_text'] = data[i]['ds_tweet_reply_graph']['to_text'].replace("|", "")

        # form a line of data
        data_line = [data[i]['ds_tweet_reply_graph']['tweet_from'],
                     data[i]['ds_tweet_reply_graph']['from_user'],
                     data[i]['ds_tweet_reply_graph']['from_create_at'],
                     data[i]['ds_tweet_reply_graph']['from_text'],
                     source[0],
                     source[1],
                     data[i]['ds_tweet_reply_graph']['tweet_to'],
                     data[i]['ds_tweet_reply_graph']['to_user'],
                     data[i]['ds_tweet_reply_graph']['to_create_at'],
                     data[i]['ds_tweet_reply_graph']['to_text'],
                     target[0],
                     target[1]]

        all_data.append(data_line)

    return all_data


def write_csv(path):
    rows = create_list_from_json(path)
    with open('user_id.csv', 'w', newline='') as csv_file:
        # csv.QUOTE_MINIMAL means only when required.
        # csv.QUOTE_NONE means that quotes are never placed around fields.
        writer = csv.writer(csv_file, delimiter='|', quoting=csv.QUOTE_MINIMAL)
        for row in rows:
            writer.writerow(row)


if __name__ == '__main__':
    write_csv('user_id.json')
