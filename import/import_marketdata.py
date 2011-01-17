import sys
import psycopg
import csv
import time
import gzip

db = psycopg.connect(database = 'evec', user = 'evec')
cur = db.cursor()

#csvtime = "2011-01-10 00:00:03.667865"
#print int(time.mktime(time.strptime(csvtime, '%Y-%m-%d %H:%M:%S.%f')))
#print sys.argv[1]
#exit()


stat = gzip.open(sys.argv[1])
# CSV format:
# orderid, regionid, systemid, stationid, typeid, bid, price, minvolume, volremain, volenter, issued, duration, range, reportedby, reportedtime
# 0        1         2         3          4       5    6      7          8          9         10      11 + 12   13     14          15

#    Column    |        Type         |         Modifiers
#--------------+-----------------------------+------------------------
#    regionid  | bigint              | not null
#    systemid  | bigint                 | not null
#    stationid | bigint                 | not null
#    typeid       | bigint                 | not null
#    bid       | integer             | not null default 0
#    price       | double precision    | not null
#    orderid   | bigint                 | not null
#    minvolume | integer             | not null
#    volremain | integer             | not null
#    volenter  | integer             | not null
#    issued    | date                | not null
#    duration  | interval            | not null
#    range     | integer             | not null
#    reportedby| bigint              | not null
#    reportedtime| timestamp without time zone | not null default now()


lines = stat.readlines()

csvread = csv.reader(lines)
for fields in csvread:

    # skip first row
    if fields[0] == "orderid" :
        continue

    orderid = long(fields[0])
    print orderid
    regionid = long(fields[1])
    systemid = long(fields[2])
    stationid = long(fields[3])
    typeid = long(fields[4])
    bid = int(fields[5])
    price = float(fields[6])
    minvolume = int(fields[7])
    volremain = int(fields[8])
    volenter = int(fields[9])
    #issued = int(time.mktime(time.strptime(fields[10], ' %Y-%m-%d %H:%M:%S ')))
    issued = fields[10]
    duration = fields[11] + fields[12]
    range = int(fields[13])
    reportedby = int(fields[14])
    #reportedtime = int(time.mktime(time.strptime(fields[15], ' %Y-%m-%d %H:%M:%S.%f ')))
    reportedtime = fields[15]

    #print orderid, regionid, systemid, stationid, typeid, bid, price, minvolume, volremain, volenter, issued, duration, range, reportedby, reportedtime

    try:
        cur.execute('DELETE FROM current_market WHERE orderid = %s', [orderid])
        db.commit()
    except:
        db.rollback()

    #try:
    #    cur.execute("UPDATE current_market SET regionid = %s, systemid = %s, stationid = %s, typeid = %s, bid = %s, price = %s, minvolume = %s, volremain = %s, volenter = %s, issued = %s, duration = '%s', range = %s, reportedby = %s, reportedtime = %s WHERE orderid = %s", [regionid, systemid, stationid, typeid, bid, price, minvolume, volremain, volenter, issued, duration, range, reportedby, reportedtime, orderid])
    #    db.commit()
    #except:
    #    db.rollback()

    #try:
    #    cur.execute("INSERT INTO current_market (orderid, regionid, systemid, stationid, typeid, bid, price, minvolume, volremain, volenter, issued, duration, range, reportedby, reportedtime) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)", [orderid, regionid, systemid, stationid, typeid, bid, price, minvolume, volremain, volenter, issued, duration, range, reportedby, reportedtime])
    #    print "Added new order ",orderid
    #    db.commit()
    #except:
    #    db.rollback()

    cur.execute("INSERT INTO current_market (orderid, regionid, systemid, stationid, typeid, bid, price, minvolume, volremain, volenter, issued, duration, range, reportedby, reportedtime) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)", [orderid, regionid, systemid, stationid, typeid, bid, price, minvolume, volremain, volenter, issued, duration, range, reportedby, reportedtime])
    db.commit()
db.commit()
