#!/usr/bin/env python

import urllib
from contextlib import closing
import unittest

MARKETSTAT = 'https://eve-central.com/api/marketstat'

class TestMarketstat(unittest.TestCase):

  def test_post(self):
    """ Simple test of POST data with two repeated parameters """
    postdata = urllib.urlencode([('typeid', '34'), ('typeid', '35')])
    with closing(urllib.urlopen(MARKETSTAT, postdata)) as f:
      data = f.read()
      self.assertTrue("<type id=\"35\">" in data)
      self.assertTrue("<type id=\"34\">" in data)

if __name__ == "__main__":
  unittest.main()
