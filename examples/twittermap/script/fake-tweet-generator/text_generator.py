import tensorflow as tf
import os
from asterixdb_connector import AsterixConnection
from textgenrnn import textgenrnn
import multiprocessing as mp
import time
import itertools
import json
import click
import numpy as np


def generate(k):
    np.random.seed()
    textgen = textgenrnn('./weights/twitter_general_weights.hdf5')
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
@click.option('--infile', '-i',
              required=True,
              help='Enter the filename storing the keyword json (e.g. local_keyword.json).')
@click.option('--outfile', '-o',
              required=True,
              help='Enter the filename to store the complete text json (e.g. local_text.json).')
def main(server, keyword, infile, outfile):
    # silence tensorflow
    tf.compat.v1.logging.set_verbosity(tf.compat.v1.logging.ERROR)
    os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'

    asterix_conn = AsterixConnection(server=server)
    
    keyword = list(keyword)
    query = 'use twitter; select value count(1) from (select d.id from ds_tweet_coord d where ftcontains(d.text, ' + str(keyword) + ", {'mode':'any'})) t;"
    response = asterix_conn.query(query)
    l = response.results[0]
    response = asterix_conn.query('select value id from twitter.ds_tweet_coord;')
    print('Generating general fake texts...')
    start_time = time.time()
    k = (len(response.results)-l) // mp.cpu_count() + 1
    pool = mp.Pool(mp.cpu_count())
    ftext = pool.map(generate, [k for _ in range(mp.cpu_count())])
    pool.close()
    ftext = list(itertools.chain.from_iterable(ftext))
    print('Generate time: {} seconds'.format(time.time() - start_time))

    with open(infile, 'r') as f:
        d = json.load(f)
    
    fake_text = []
    i = 0
    for s in response.results:
        flag = True
        for w in d.keys():
            if str(s) in d[w].keys():
                text = d[w][str(s)]
                flag = False
                break
        if flag:
            text = ftext[i]
            i += 1
        fake_text.append({'id': s, 'text': text})

    with open(outfile, 'w') as f:
        json.dump(fake_text, f)


if __name__ == '__main__':
    main()
