from asterixdb_connector import AsterixConnection
from textgenrnn import textgenrnn
import multiprocessing as mp
import itertools
import time
import json
import click

def generate(k,w):
    textgen = textgenrnn('./weights/twitter_{}_weights.hdf5'.format(w))
    text = textgen.generate(n = k, max_gen_length = 140, return_as_list = True)
    return text


@click.command()
@click.option('--server', '-s',
              required=True,
              help='Enter the url of the AsterixDB server.')
@click.option('--keyword', '-w',
              multiple=True,
              required=True,
              help='Enter the keywords to generate tweets.')
@click.option('--outfile', '-o',
              required=True,
              help='Enter the filename to store the keyword json (e.g. local_keyword.json).')
def main(server, keyword, outfile):
    print('Connection to server: ' + server + '...')
    asterix_conn = AsterixConnection(server = server)
    
    print('Generating fake texts with keywords...')
    start_time = time.time()
    d = dict()
    for w in keyword:
        print('Generating texts for keyword {}...'.format(w))
        query_string = "use twitter; select value d.id from ds_tweet_coord d where ftcontains(d.text, ['" + w + "'], {'mode':'any'});"
        response = asterix_conn.query(query_string)
        id_list = response.results
        k = len(id_list) // mp.cpu_count() + 1
        pool = mp.Pool(mp.cpu_count())
        text = pool.starmap(generate, zip([k for i in range(mp.cpu_count())],[w for i in range(mp.cpu_count())]))
        pool.close()
        text = list(itertools.chain.from_iterable(text))
        d[w] = dict(zip(id_list, text[:len(id_list)]))
    print('Generate time: {} seconds'.format(time.time() - start_time))

    with open(outfile,'w') as f:
        json.dump(d, f)


if __name__ == '__main__':
    main()
