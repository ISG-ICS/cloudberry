from urllib import parse, request
import json


class QueryResponse:
    def __init__(self, raw_response):
        self._json = json.loads(raw_response)

        self.requestID = self._json['requestID'] if 'requestID' in self._json else None
        self.clientContextID = self._json['clientContextID'] if 'clientContextID' in self._json else None
        self.signature = self._json['signature'] if 'signature' in self._json else None
        self.results = self._json['results'] if 'results' in self. _json else None
        self.metrics = self._json['metrics'] if 'metrics' in self._json else None


class AsterixConnection:
    def __init__(self, server='http://localhost', port=19002):
        self._server = server
        self._port = port
        self._url_base = self._server + ':' + str(port)
        print('Connecting to ' + self._url_base + '...')

    def query(self, statement, pretty=False, client_context_id=None):
        endpoint = '/query/service'

        url = self._url_base + endpoint

        payload = {
            'statement': statement,
            'pretty': pretty
        }

        if client_context_id:
            payload['client_context_id'] = client_context_id

        data = parse.urlencode(payload).encode("utf-8")
        req = request.Request(url, data)
        response = request.urlopen(req).read()

        return QueryResponse(response)
