import re
from textgenrnn import textgenrnn
from asterixdb_connector import AsterixConnection
import time
from tqdm import tqdm
import click

def process_tweet_text(text):
    text = re.sub(r'http\S+', '', text)   # Remove URLs
    text = re.sub(r'@[a-zA-Z0-9_]+', '', text)  # Remove @ mentions
    text = text.strip(" ")   # Remove whitespace resulting from above
    text = re.sub(r' +', ' ', text)   # Remove redundant spaces
    
    # Handle common HTML entities
    text = re.sub(r'&lt;', '<', text)
    text = re.sub(r'&gt;', '>', text)
    text = re.sub(r'&amp;', '&', text)
    return text

def train_model(server, keyword, k):
    cfg = {'num_epochs': 5,
           'gen_epochs': 1,
           'batch_size': 128,
           'train_size': 1.0,
           'new_model': False,
           'model_config': {'rnn_layers': 2,
                            'rnn_size': 128,
                            'rnn_bidirectional': False,
                            'max_length': 40,
                            'dim_embeddings': 100,
                            'word_level': False
                            }
           }
    texts = []
    context_labels = []

    print('Connection to server: ' + server + '...')    
    asterix_conn = AsterixConnection(server = server)
    query_string = 'use twitter; select d.user.screen_name, d.text from ds_tweet_coord d'

    if keyword is not None:
        count_query = "use twitter; select value count(1) from ds_tweet_coord d where ftcontains(d.text, ['" + keyword + "'], {'mode':'any'});"
        response = asterix_conn.query(count_query)
        query_string += " where ftcontains(d.text, ['" + keyword + "'], {'mode':'any'})" + " and random() < {};".format(k / response.results[0])            
    else:
        count_query = 'use twitter; select value count(1) from ds_tweet_coord d;'
        response = asterix_conn.query(count_query)
        query_string += ' where random() < {};'.format(k / response.results[0])

    print('Query:', query_string)
    response = asterix_conn.query(query_string)
    print('Actual size:', len(response.results))
    
    for s in response.results:
        tweet_text = process_tweet_text(s['text'])
        if tweet_text is not '':
            texts.append(tweet_text)
            context_labels.append(s['screen_name'])
    
    textgen = textgenrnn(name='./weights/twitter_{}'.format(keyword if keyword is not None else 'general'))
    
    if cfg['new_model']:
        textgen.train_new_model(
            texts,
            context_labels=context_labels,
            num_epochs=cfg['num_epochs'],
            gen_epochs=cfg['gen_epochs'],
            batch_size=cfg['batch_size'],
            train_size=cfg['train_size'],
            rnn_layers=cfg['model_config']['rnn_layers'],
            rnn_size=cfg['model_config']['rnn_size'],
            rnn_bidirectional=cfg['model_config']['rnn_bidirectional'],
            max_length=cfg['model_config']['max_length'],
            dim_embeddings=cfg['model_config']['dim_embeddings'],
            word_level=cfg['model_config']['word_level'])
    else:
        textgen.train_on_texts(
            texts,
            context_labels=context_labels,
            num_epochs=cfg['num_epochs'],
            gen_epochs=cfg['gen_epochs'],
            train_size=cfg['train_size'],
            batch_size=cfg['batch_size'])


@click.command()
@click.option('--server', '-s',
              required=True,
              help='Enter the url of the AsterixDB server.')
@click.option('--keyword', '-w',
              multiple=True,
              required=True,
              help='Enter the keywords to be trained.')
@click.option('--size', '-k', type = click.INT,
              required=True,
              help='Enter the training sample size.')
def main(server, keyword, size):
    # training tweets with keyword
    for w in tqdm(keyword):
        print('Training tweets with keyword {} and sample size k = {}...'.format(w, size))
        start_time = time.time()
        try:
            train_model(server = server, keyword = w, k = size)
        except ValueError:
            pass
        print("Training time: {} seconds".format(time.time() - start_time))

    # training general tweets
    print('Training general tweets with sample size k = {}...'.format(size))
    start_time = time.time()
    try:
        train_model(server = server, keyword = None, k = size)
    except ValueError:
        pass
    print("Training time: {} seconds".format(time.time() - start_time))

    print('Finished training all models!')


if __name__ == '__main__':
    main()
