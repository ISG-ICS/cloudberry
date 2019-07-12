from asterixdb_connector import AsterixConnection
from pynamesgenerator import gen_two_words, gen_year
import multiprocessing as mp
import time
import click
from tqdm import tqdm


def generate(s):
    name = gen_two_words(split=' ', lowercase=False)
    screenname = ''.join(name.split(' ')).lower() + gen_year(10000, 99999)
    return {'id': s, 'name': name, 'screen_name': screenname}


@click.command()
@click.option('--server', '-s',
              required=True,
              help='Enter the url of the AsterixDB server.')
def main(server):
    asterix_conn = AsterixConnection(server=server)
    
    response = asterix_conn.query('select distinct value user.id from twitter.ds_tweet_coord;')
    print('Generating fake names...')
    start_time = time.time()
    pool = mp.Pool(mp.cpu_count())
    fake_name = pool.map(generate, [s for s in response.results])
    pool.close()
    print('Generate time: {} seconds'.format(time.time() - start_time))
    
    asterix_conn.query('''use twitter;
        create type typeFname if not exists as open {
            id: int64,
            name: string,
            screen_name: string
        };
        drop dataset fname if exists;
        create dataset fname (typeFname) if not exists primary key id;''')    
    print('Inserting fake names into database...')
    start_time = time.time()
    n = 10000
    for i in tqdm(range(0, len(fake_name), n)):
        insert_query = 'use twitter; insert into fname({});'.format(fake_name[i:min(len(fake_name), i+n)])
        print('Inserting records {} to {}...'.format(i+1, min(len(fake_name), i+n)))
        asterix_conn.query(insert_query)
    print('Insert time: {} seconds'.format(time.time() - start_time))


if __name__ == '__main__':
    main()
