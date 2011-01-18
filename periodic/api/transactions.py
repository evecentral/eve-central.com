import sys
sys.path.append('../../lib/')

import psycopg2
from mx.DateTime import *
import datetime

from evecentral.apilib import ApiRequestor
from evecentral.userlib import *

import logging
import logging.handlers



def update_last_transid(userid, char_lasttrans, corp_lasttrans):
    cur = db.cursor()
    if char_lasttrans is None:
        char_lasttrans = 0
    if corp_lasttrans is None:
        corp_lasttrans = 0
    logger.debug("Setting user's last transIDs: %d %d", char_lasttrans, corp_lasttrans)
    
    cur.execute("DELETE FROM api_market_transid WHERE userid = %d", [int(userid)])
    cur.execute("INSERT INTO api_market_transid (userid, char_lasttrans, corp_lasttrans) VALUES (%s, %s, %s)", [userid, char_lasttrans, corp_lasttrans])
    db.commit()

def fetch_and_validate(u, mode = 'char'):
    apir = ApiRequestor(u, '/' + mode + '/WalletTransactions.xml.aspx')
    xdom = None
    data = None
    try:
        (xdom, data) = apir.fetch()
    except Exception,e:
        logger.error("Fetch error due to internal error: %s.", e)
        return None
    
    cachedUntil = DateTimeFrom(xdom.documentElement.getElementsByTagName("cachedUntil").item(0).firstChild.data)
    logger.debug("Response has cachedUntil: %s", cachedUntil)
    if cachedUntil >= u.api_cacheuntil:
        logger.debug("Updating cacheduntil in database")
        u.api_cacheuntil = cachedUntil
        u.update_user(db)

    errorCode = xdom.documentElement.getElementsByTagName("error")
    if errorCode:
        try:
            errorCode = int(errorCode.item(0).attributes.item(0).nodeValue)
            logger.error("Error code is %d", errorCode)
            if (errorCode >= 200 and errorCode <= 205) or errorCode == 211:
                # This is an authentication error - clear the API key since things are broken
                logger.error("Disabling account")
                            #u.full_apikey = None
                        #u.update_user(db)
            return None
        except:
            print "ERROR: ",data
            
    return xdom

#     <row transactionDateTime="2009-03-23 20:18:00" transactionID="924193883" quantity="1" typeName="Bustard" typeID="12731" price="71000000.00" clientID="468549928" clientName="PopinJay" stationID="60003874" stationName="Nonni V - Caldari Navy Assembly Plant" transactionType="buy" transactionFor="personal" />

def process_xdom(results, hightrans, userid, accountkey):
    lasttrans = 0
    for result in results:
        txtime = str(result.getAttribute("transactionDateTime"))
        txid = long(str(result.getAttribute("transactionID")))
        qty = str(result.getAttribute("quantity"))
        typeName = str(result.getAttribute("typeName"))
        typeId = str(result.getAttribute("typeID"))
        price = str(result.getAttribute("price"))
        clientId = str(result.getAttribute("clientID"))
        clientName = ""
        try:
            clientName = str(result.getAttribute("clientName"))
        except:
            pass
        
        stationId = str(result.getAttribute("stationID"))
        stationName = str(result.getAttribute("stationName"))
        txtype = str(result.getAttribute("transactionType"))
        txfor = str(result.getAttribute("transactionFor"))
        charname = ""
        charid = 0
        try:
            charname = str(result.getAttribute("characterName"))
            charid = str(result.getAttribute("characterID"))
            if charid == '':
                charid = 0

        except:
            pass

            
        iscorp = True
        if txfor == "personal":
            iscorp = False

        if txid >= lasttrans:
            lasttrans = txid
        
        if txid <= hightrans:
            #print "Skip txid",txid,"<=",hightrans,txtime
            continue

        
        logger.debug("New item: %s %s %s",txtime,typeName,price)
        cur = db.cursor()
        vals = [userid, accountkey, txtime, txid, qty, typeName, typeId, price, clientId, clientName, charid, charname, stationId, stationName, txtype, iscorp] 
        
        cur.execute("INSERT INTO wallet_market_transactions (userid, accountkey, transtime, transactionid, quantity, typename, typeid, price, clientid, clientname, characterid, charactername, stationid, stationname, transactiontype, corp) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)", vals)
        db.commit()
    return lasttrans

def username_fetch(username):
    u = User.fakelogin(db, username)
    nowt = now()
    logger.debug("--- %s CachedUntil:%s Now:%s",username,u.api_cacheuntil,nowt)
    
    if u.api_cacheuntil > nowt:
        logger.debug(" Skipping until %s ",u.api_cacheuntil)
        return

    highcharid = 0
    highcorpid = 0
    cur = db.cursor()
    cur.execute("SELECT char_lasttrans, corp_lasttrans FROM api_market_transid WHERE userid = %s", [u.userid])
    r = cur.fetchone()
    try:
        highcharid = r[0]
        highcorpid = r[1]
    except:
        logger.debug("Couldn't fetch high IDs")

    logger.debug("Loaded last trans IDS: %d %d", highcharid, highcorpid)
    
    for mode in ['char', 'corp']:
        logger.debug("Mode: %s ",mode)
        xdom = fetch_and_validate(u, mode = mode)
        if xdom is None:
            continue
        
        # Now we process information
        results = xdom.documentElement.getElementsByTagName("row")
        logger.debug("# of results: %s ",len(results))
        highid = 0
        if mode == 'corp':
            highid = highcorpid
        else:
            highid = highcharid
        
        highid = process_xdom(results, highid, u.userid, 0)
        
        if mode == 'corp':
            highcorpid = highid
        else:
            highcharid = highid

    update_last_transid(u.userid, highcharid, highcorpid)
    
    


def main():
    global logger, db

    logger = logging.getLogger("transactions")
    ch = logging.handlers.RotatingFileHandler("transactions.log", maxBytes=1024*1024*100, backupCount = 5)
    formatter = logging.Formatter("%(asctime)s - %(name)s - %(levelname)s - %(message)s")
    ch.setFormatter(formatter)
    logger.addHandler(ch)
    logger.setLevel(logging.DEBUG)
    
    logger.debug("-------------------------")
    logger.debug("Beginning run")
    
    db = psycopg2.connect(database='evec', user='evec', host = 'localhost', port = '9999')

    cur = db.cursor()
    cur.execute("SELECT username FROM users WHERE full_apikey IS NOT NULL OR full_apikey != ''")

    r = cur.fetchone()
    while r:
        username = r[0]
        username_fetch(username)
        r = cur.fetchone()

    logger.debug("Archiving old data from 7 days")
    cur = db.cursor()
    cur.execute("insert into archive_transactions select * from wallet_market_transactions where transtime < NOW() - interval '7 days'");
    cur.execute("delete from wallet_market_transactions where transtime < NOW() - interval '7 days'");
    db.commit()
    logger.debug("DONE!")

if __name__ == "__main__":
    main()

    

