from Queue import Queue
import threading
from hashlib import md5
import string
import os
import Cheetah.Template
import smtplib

import cherrypy

from evecentral import display
from evecentral import evec_func
from evecentral.userlib import User
from evecentral import cache
from evecentral import stats

class BackgroundStatThread(threading.Thread):
    """
    A background worker thread, handles statistic generation events from
    an upload and recomputes per (region,typeid) stats.
    """

    def __init__(self, inqueue):
        self.queue = inqueue
        threading.Thread.__init__(self)

    def perform_stat(self):
        regionid, typeid = self.queue.get()
        db = evec_func.db_con()

        minq = 0
        if typeid in stats.MINQ_TYPES:
            minq = stats.MINQ_VOL

        (buy,sell) = stats.item_stat(db, typeid, regionlimit = [regionid], nocache = True, minQ = minq)
        all_stat = stats.item_stat(db, typeid, regionlimit = [regionid], nocache = True, minQ = minq, buysell = False)
        cur = db.cursor()
        # Buyup not yet available in stats
        #print "DEBUG stat: ",typeid,regionid,all_stat
        cur.execute("INSERT INTO trends_type_region (typeid, region, average, median, volume, stddev, buyup, timeat) VALUES (%s, %s, %s, %s, %s, %s, %s, NOW())",
                    (typeid, regionid, float(all_stat['avg_price']), float(all_stat['median']), float(all_stat['total_vol']), float(all_stat['stddev']), float(0)))
        db.commit()

        self.queue.task_done()
        
    def run(self):
        while True:
            try:
                self.perform_stat()
            except Exception,e:
                print e
                
class DataInput:

    def __init__(self):
        self.stat_queue = Queue()
        self.run_thread = BackgroundStatThread(self.stat_queue)
        self.run_thread.start()

    @cherrypy.expose
    def userlogin(self, username, password):

        db = evec_func.db_con()
        cur = db.cursor()

        cur.execute('SELECT userid FROM users WHERE username = %s AND password = md5(%s)',
                    [username, User.salt(password, username)])
        r = cur.fetchone()

        if r:
            return str(r[0])
        else:
            return '-1'



        db.close()

    @cherrypy.expose
    def usercount(self, userid):

        db = evec_func.db_con()
        cur = db.cursor()

        cur.execute('SELECT uploads FROM users WHERE userid = %s',
                    [userid])
        r = cur.fetchone()

        if r:
            return str(r[0])
        else:
            return '0'

        db.close()

    def station_check(self, db, station, system, region):
        cur = db.cursor()
        cur.execute("SELECT systemid FROM systems WHERE systemid = %s", [system])
        if not cur.fetchone():
            raise "No system by systemid " + `system` + " region " + `region`

        cur.execute("SELECT stationid FROM stations WHERE stationid = %s", [station])
        if not cur.fetchone():
            pass
        else:
            return # Station exists

        cur.execute("SELECT systemname FROM systems WHERE systemid = %s", [system])
        r = cur.fetchone()
        newname = r[0] + " Player Outpost (no name provided yet)"
        cur.execute("INSERT INTO stations (stationid, stationname, systemid) VALUES (%s, %s, %s)", [station, newname, system])


    @cherrypy.expose
    def inputdata(self,typename=None,userid=None,data=None,**kw):

        response = ""

        if userid is None:
            userid = 0
        else:
            userid = long(userid)

        db = evec_func.db_con()

        if data is None:
            return ""

        # Send e-mail to the update push list




        #smtpsess = smtplib.SMTP('localhost')
        #datae = "To: evec-upload@lists.stackworks.net\nPrecedence: bulk\nX-EVEC-UserIdHash: " + hexdigest + "\nSubject: Upload\n\n" + data
        #smtpres = smtpsess.sendmail('uploader@stackworks.net', 'evec-upload@lists.stackworks.net', datae);


        data = data.split("\n")

        del data[0] # header
        ndata = []
        mailcount = cache.incr("evec_mail_count")
        cache.set("evec_mail_" + str(mailcount), "\n".join(data), expire = 3600)


        # chunk the CSV file

        for line in data:
            if len(line) > 1:
                ndata.append(line.split(','))
        data = ndata

        cur = db.cursor()

        try:
            line = data[0]
            typeid = line[2]
            region = line[11]
        except:
            return

        response += "Beginning your upload of "+typename+"\n"
        response += "TypeID: " + typeid + " RegionID: " + region + "\n"
        #cur.execute('SET TRANSACTION ISOLATION LEVEL READ COMMITTED')

        # Pre-validate the data coming in
        for line in data:
            if long(typeid) != long(line[2]):
                print "REJECT due to mismatched typeids"
                return # fail
            cur.execute('SELECT typeid,regionid FROM current_market WHERE orderid = %s', [line[4]])
            r = cur.fetchone()
            if r:
                if long(r[0]) != long(typeid) or long(r[1]) != long(region):
                    print "REJECT due to mismatched typeids in DB - type",r[0],"==",typeid," or ",r[1], "==", region
                    return # invalid data


        #cur.execute('LOCK TABLE users IN SHARE ROW EXCLUSIVE MODE')

        #cur.execute('UPDATE users SET uploads = uploads + 1 WHERE userid = %s', [userid])
        #db.commit()


        cur.execute('DELETE FROM current_market WHERE typeid = %s AND regionid = %s', [typeid, region])
        #db.commit()


        derived_region = 0
        derived_typeid = 0
        has_new_data = False

        for line in data:
            typeid = line[2]
            region = line[11]

            derived_typeid = typeid
            derived_region = region

            bid = line[7]
            if bid == "True":
                bid = 1
            else:
                bid = 0

            station = line[10]
            system = line[12]
            source = "evec_upload_cache"

            try:
                source = line[14]
            except:
                pass

            self.station_check(db, station = station, system = system, region = region)


            cur.execute("""
            INSERT INTO current_market (regionid, systemid, stationid, typeid,
            bid,price, orderid, minvolume, volremain, volenter, issued, duration, range, reportedby)
            VALUES( %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)


            """, [region, line[12], line[10], typeid, `bid`, line[0], line[4], line[6],
                  int(float(line[1])), line[5], line[8], line[9]+" days", line[3], 0])

            cur.execute("SELECT count(orderid) FROM archive_market WHERE orderid = %s AND volremain = %s AND regionid = %s", [ line[4], int(float(line[1])), region])
            hasdata = cur.fetchone()
            hasdata = hasdata[0]
            if hasdata == 0:
                has_new_data = True
                cur.execute("""
                            INSERT INTO archive_market (regionid, systemid, stationid, typeid,
                            bid,price, orderid, minvolume, volremain, volenter, issued, duration, range, reportedby, source)
                            VALUES( %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                            """, [region, line[12], line[10], typeid, `bid`, line[0], line[4], line[6],
                                  int(float(line[1])), line[5], line[8], line[9]+" days", line[3], 0, source])


        db.commit()
        db.close()
        response += "Complete! Thank you for your contribution to EVE-Central.com!"
        if derived_region != 0 and derived_typeid != 0:
            # Schedule a background queue action to recompute statistics
            self.stat_queue.put((region, typeid))


        return response
