from evecentral.apilib import *

class Wallet(object):
    def __init__(self, user, db, corp = False):
	self.user = user
	self.db = db
	self.account = {}


    def get_wallet_balance(self, account):
	cur = self.db.cursor()
	cur.execute("SELECT balance,timeat FROM wallet WHERE userid = %s AND walletkey = %s AND timeat > NOW () - interval '15 minutes'",
		    [self.user.userid, account])
	r = cur.fetchone()
	if r:
	    return (r[0],r[1])
	else:
	    self.get_live_wallet()
	    cur.execute("SELECT balance,timeat FROM wallet WHERE userid = %s AND walletkey = %s AND timeat > NOW () - interval '15 minutes'",
			    [self.user.userid, account])
	    r = cur.fetchone()
	    return (r[0], r[1])

    def get_live_wallet(self):
	try:
	    apir = ApiRequestor(self.user, '/char/AccountBalance.xml.aspx')
	    xdom = apir.fetch( )
	    if xdom is None:
		raise ApiError()
	    attr =  xdom.documentElement.getElementsByTagName("result").item(0).firstChild.childNodes[0].attributes
	    result = {}
	    for x in range(0, attr.length):
		result[attr.item(x).name] = attr.item(x).nodeValue
	    self.account[int(result['accountKey'])] = result['balance']
	    xdom.unlink()
	    cur = self.db.cursor()
	    cur.execute("INSERT INTO wallet (userid, walletkey, balance, timeat) VALUES (%s, %s, %s, NOW())",
			[self.user.userid, 1000, self.account[1000]])
	    self.db.commit()
	except:
	    self.account[1000] = -100

    def get_wallet_history(self, account, number = 100, sort = "DESC"):
	bals = []
	cur = self.db.cursor()
	cur.execute("SELECT balance,timeat FROM wallet WHERE userid = %s AND walletkey = %s ORDER BY timeat " + sort + " LIMIT " + `number` + " OFFSET 0",
		    [self.user.userid, account])
	r = cur.fetchone()
	while r:
	    bals.append((r[0], r[1]))
	    r = cur.fetchone()
	return bals
