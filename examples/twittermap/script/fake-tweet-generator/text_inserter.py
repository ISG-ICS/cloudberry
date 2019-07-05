from asterixdb_connector import AsterixConnection
import time
import json
from tqdm import tqdm
import click

@click.command()
@click.option('--server', '-s',
              required=True,
              help='Enter the url of the AsterixDB server.')
@click.option('--infile', '-i',
              required=True,
              help='Enter the filename storing the complete text json (e.g. local_text.json).')
def main(server, infile):
    print('Connection to server: ' + server + '...')
    asterix_conn = AsterixConnection(server = server)
    
    with open(infile,'r') as f:
        fake_text = json.load(f)
    
    response = asterix_conn.query('''use twitter;
        create type typeFtext if not exists as open {
            id: int64,
            text: string
        };
        drop dataset ftext if exists;
        create dataset ftext (typeFtext) if not exists primary key id;''')
    print('Inserting fake texts into database...')
    start_time = time.time()
    for i in tqdm(range(0,len(fake_text),200000)):
        insert_query = 'use twitter; insert into ftext({});'.format(fake_text[i:min(len(fake_text),i+200000)])
        print('Inserting records {} to {}...'.format(i+1,min(len(fake_text),i+200000)))
        response = asterix_conn.query(insert_query)
    print('Insert time: {} seconds'.format(time.time() - start_time))


if __name__ == '__main__':
    main()
