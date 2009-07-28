import httplib, urllib
from xml.dom.minidom import parseString

class EVECUrllibRequesttor(urllib.FancyURLopener):
    version = 'EVE-Central.com API Fetch-o-Matic 1.0'

urllib._urlopener = EVECUrllibRequesttor()



class ApiError(Exception):
    pass

class ApiRequestor(object):
    def __init__(self, user, path):
        self.user = user
        self.path = path
    def fetch(self, **params):
        #if params == None:
        params = {}
        params['apikey'] = self.user.full_apikey
        params['userid'] = self.user.apiuserid
        params['characterID'] = self.user.userid
        headers = {
                    "Content-type": "application/x-www-form-urlencoded",
                    'User-Agent' : 'EVE-Central.com API Fetch-o-Matic 1.0 (atrus@stackworks.net)'}

        conn = httplib.HTTPConnection("api.eve-online.com")
        conn.request("POST", self.path, urllib.urlencode(params), headers)

        # now get response from server, print out the status code for debugging
        response = conn.getresponse()
        if response.status != 200:
            return None
        data = response.read()


        xdom = parseString(data)
        return (xdom,data)
    def test2(self):
        f = open('raw', 'r')
        l = f.readlines()
        s = ""
        for line in  l:
            s += line
        return (parseString(s), s)

   
