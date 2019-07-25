from asterixdb_connector import AsterixConnection
import time
import json
import click
import urllib
from tqdm import tqdm


@click.command()
@click.option('--server', '-s',
              required=True,
              help='Enter the url of the AsterixDB server.')
@click.option('--infile', '-i',
              required=True,
              help='Enter the filename storing the complete text json (e.g. local_text.json).')
def main(server, infile):
    asterix_conn = AsterixConnection(server=server)
    
    with open(infile,'r') as f:
        fake_text = json.load(f)

    start_time = time.time()
    for i in range(len(fake_text)):
        fake_text[i]['text'] = fake_text[i]['text'].encode(encoding='ascii', errors='ignore').decode().replace('\x7f', '')
    print('Pre-process time: {} seconds'.format(time.time() - start_time))

    asterix_conn.query('''use twitter;
        create type typeFtext if not exists as open {
            id: int64,
            text: string
        };
        drop dataset ftext if exists;
        create dataset ftext (typeFtext) if not exists primary key id;''')
    print('Inserting fake texts into database...')
    start_time = time.time()
    n = 10000
    for i in tqdm(range(0, len(fake_text), n)):
        print('Inserting records {} to {}...'.format(i + 1, min(len(fake_text), i + n)))
        insert_query = 'use twitter; insert into ftext({});'.format(fake_text[i:min(len(fake_text), i+n)])
        try:
            asterix_conn.query(insert_query)
        except urllib.error.HTTPError:
            print(insert_query)
            break
    print('Insert time: {} seconds'.format(time.time() - start_time))


if __name__ == '__main__':
    main()
