#    EVE-Central.com Codebase
#    Copyright (C) 2006-2009 StackFoundry LLC and Yann Ramin
#
#    This program is free software: you can redistribute it and/or modify
#    it under the terms of the GNU Affero General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU Affero General Public License for more details.
#
#    You should have received a copy of the GNU Affero General Public License
#    along with this program.  If not, see <http://www.gnu.org/licenses/>.


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
