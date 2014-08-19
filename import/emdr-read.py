from contextlib import closing

import zlib
import zmq
# You can substitute the stdlib's json module, if that suits your fancy
import json
import urllib
import sys

def main(server_host, zmq_host):
    context = zmq.Context()
    subscriber = context.socket(zmq.SUB)

    # Connect to the first publicly available relay.
    subscriber.connect('tcp://' + zmq_host + ':8050')
    # Disable filtering.
    subscriber.setsockopt(zmq.SUBSCRIBE, "")

    while True:
        raw = subscriber.recv()
        market_json = zlib.decompress(raw)
        market_data = json.loads(market_json)
        with closing(urllib.urlopen('http://' + server_host + '/api/upload', urllib.urlencode({'data': market_json}))) as req:
          sys.stdout.write('.')
          sys.stdout.flush()


if __name__ == '__main__':
  server_host = sys.argv[1]
  zmq_host = sys.argv[2]
  while True:
    try:
      main(server_host, zmq_host)
    except Exception as e:
      print e
