import json
import csv

# for float comparison
eps = 1e-5

# status = 1, 2, 3, 4
status = 1


def detail_error_detect(data, label):
    try:
        _ = data[0]['ds_tweet_reply_graph'][label]
        return True
    except KeyError:
        return False


def key_error_detect(data):
    global status
    has_from_coordinate = detail_error_detect(data, 'from_coordinate')
    has_to_coordinate = detail_error_detect(data, 'to_coordinate')
    if has_from_coordinate and has_to_coordinate:
        status = 1
    elif has_from_coordinate and not has_to_coordinate:
        status = 2
    elif not has_from_coordinate and has_to_coordinate:
        status = 3
    else:
        status = 4


def line_strip(line):
    # strip out extra white spaces, commas and brackets
    line = line.strip()
    if line.startswith('['):
        line = line.lstrip('[')
    if line.startswith(','):
        line = line.lstrip(',')
    if line.endswith(']'):
        line = line.rstrip(']')
    line = '[' + line + ']'
    return line


def create_csv_from_json(json_file):
    with open(json_file) as f, open('user_id.csv', 'w', newline='') as csv_file:
        writer = csv.writer(csv_file, delimiter='|', quoting=csv.QUOTE_MINIMAL)
        for line in f:
            line = line_strip(line)
            data = json.loads(line)
            try:
                _ = data[0]
            except IndexError:
                continue
            key_error_detect(data)
            x_from_box = (data[0]['ds_tweet_reply_graph']['from_bounding_box'][0][0] +
                          data[0]['ds_tweet_reply_graph']['from_bounding_box'][1][0]) / 2
            x_to_box = (data[0]['ds_tweet_reply_graph']['to_bounding_box'][0][0] +
                        data[0]['ds_tweet_reply_graph']['to_bounding_box'][1][0]) / 2
            y_from_box = (data[0]['ds_tweet_reply_graph']['from_bounding_box'][0][1] +
                          data[0]['ds_tweet_reply_graph']['from_bounding_box'][1][1]) / 2
            y_to_box = (data[0]['ds_tweet_reply_graph']['to_bounding_box'][0][1] +
                        data[0]['ds_tweet_reply_graph']['to_bounding_box'][1][1]) / 2
            source = []
            target = []
            # if source and target are the same, don't add the record
            if status == 1:
                source = [data[0]['ds_tweet_reply_graph']['from_coordinate'][0],
                          data[0]['ds_tweet_reply_graph']['from_coordinate'][1]]
                target = [data[0]['ds_tweet_reply_graph']['to_coordinate'][0],
                          data[0]['ds_tweet_reply_graph']['to_coordinate'][1]]
            elif status == 2:
                source = [data[0]['ds_tweet_reply_graph']['from_coordinate'][0],
                          data[0]['ds_tweet_reply_graph']['from_coordinate'][1]]
                target = [x_to_box, y_to_box]
            elif status == 3:
                source = [x_from_box, y_from_box]
                target = [data[0]['ds_tweet_reply_graph']['to_coordinate'][0],
                          data[0]['ds_tweet_reply_graph']['to_coordinate'][1]]
            elif status == 4:
                source = [x_from_box, y_from_box]
                target = [x_to_box, y_to_box]
            if abs(source[0] - target[0]) <= eps \
                    and abs(source[1] - target[1]) <= eps:
                continue

            # ignore "\n" and "|" in texts
            data[0]['ds_tweet_reply_graph']['from_text'] = data[0]['ds_tweet_reply_graph']['from_text'].replace(
                "\n", " ")
            data[0]['ds_tweet_reply_graph']['from_text'] = data[0]['ds_tweet_reply_graph']['from_text'].replace("|",
                                                                                                                " ")
            data[0]['ds_tweet_reply_graph']['to_text'] = data[0]['ds_tweet_reply_graph']['to_text'].replace("\n",
                                                                                                            " ")
            data[0]['ds_tweet_reply_graph']['to_text'] = data[0]['ds_tweet_reply_graph']['to_text'].replace("|", " ")

            # form a line of data[0]
            data_line = [data[0]['ds_tweet_reply_graph']['tweet_from'],
                         data[0]['ds_tweet_reply_graph']['from_create_at'],
                         data[0]['ds_tweet_reply_graph']['from_text'],
                         source[0],
                         source[1],
                         data[0]['ds_tweet_reply_graph']['tweet_to'],
                         data[0]['ds_tweet_reply_graph']['to_create_at'],
                         data[0]['ds_tweet_reply_graph']['to_text'],
                         target[0],
                         target[1]]

            writer.writerow(data_line)


if __name__ == '__main__':
    create_csv_from_json('user_id.json')
