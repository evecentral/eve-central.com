#!/usr/bin/env python
import psycopg2
import sqlite3
import urllib
import xml.etree.ElementTree as ET
import sys
import json

def import_jumps(dbs, db):
    cur = db.cursor()
    curs = dbs.cursor()
    curs.execute('SELECT fromRegionID,fromConstellationID,fromSolarSystemID,toSolarSystemID,toConstellationID,toRegionID FROM mapSolarSystemJumps')
    for row in curs:
        print row[2],"to",row[3]
        cur.execute('SELECT fromsystem FROM jumps WHERE fromsystem = %s AND tosystem = %s', (row[2], row[3]))
        r = cur.fetchone()
        if not r:
            cur.execute('INSERT INTO jumps (fromregion, fromconstellation, fromsystem, tosystem, toconstellation, toregion) VALUES (%s,%s,%s,%s,%s,%s)',
                        (row[0], row[1], row[2], row[3], row[4], row[5]))
            print "INSERT"
        db.commit()
        

def import_regions(dbs,db):
    cur = db.cursor()
    curs = dbs.cursor()
    curs.execute('SELECT regionID,regionName FROM mapRegions')
    for row in curs:
        print row[0],'=',row[1]
        cur.execute('SELECT regionid FROM regions WHERE regionid = %s', (row[0],))
        r = cur.fetchone()
        if r:
            cur.execute('UPDATE regions SET regionname = %s WHERE regionid = %s', (row[1], row[0]))
        else:
            cur.execute('INSERT INTO regions (regionid, regionname) VALUES (%s, %s)', (row[0], row[1]))
            print "INSERT"
        db.commit()


def import_systems(dbs,db):
    cur = db.cursor()
    curs = dbs.cursor()
    curs.execute('SELECT solarSystemID,solarSystemName,regionID,constellationID,security,factionID FROM mapSolarSystems')
    for row in curs:
        faction = row[5]
        if not faction:
            faction = 0
        print row[0],'=',row[1], " faction ", faction
        cur.execute('SELECT systemid FROM systems WHERE systemid = %s', (row[0],))
        r = cur.fetchone()
        if r:
            cur.execute('UPDATE systems SET systemname = %s, security = %s, truesec = %s, constellationid = %s, faction = %s WHERE systemid = %s', (row[1], round(float(row[4]), 1), row[4], row[3], faction, row[0]))
        else:
            cur.execute('INSERT INTO systems (systemid, systemname, regionid, faction, security, constellationid, truesec) VALUES (%s, %s, %s, %s, %s, %s, %s)', (row[0], row[1], row[2], faction, row[4], row[3], row[4]))
            print "INSERT"
        db.commit()

def import_station(db, stationid, stationname, solarsystem):
    cur = db.cursor()
    cur.execute('SELECT stationname FROM stations WHERE stationid = %s', (stationid,))
    r = cur.fetchone()
    if r:
        cur.execute('UPDATE stations SET stationname = %s, systemid = %s WHERE stationid = %s', (stationname, solarsystem, stationid))
    else:
        cur.execute('INSERT INTO stations (stationid, stationname, systemid) VALUES (%s, %s, %s)', (stationid, stationname, solarsystem))
        print "INSERT"
    db.commit()

def import_stations(dbs,db):
    cur = db.cursor()
    curs = dbs.cursor()
    curs.execute('SELECT stationID, stationName, solarSystemID from staStations')
    static_stations = []
    for row in curs:
        stationid = row[0]
        stationname = row[1]
        solarsystem = row[2]
        station = {'id': int(stationid), 'name': stationname, 'solarsystem': int(solarsystem)}
        static_stations.append(station)
        print row[0],'=',row[1]
        import_station(db, stationid, stationname, solarsystem)
    with open('static_stations.json', 'w') as f:
        print >>f, json.dumps(static_stations)

def import_conq_stations(db):
    url = "https://api.eveonline.com/eve/ConquerableStationList.xml.aspx"
    xml_string = urllib.urlopen(url).read()
    tree = ET.fromstring(xml_string)
    conq_stations = []
    for row in tree.iter('row'):
        stationid = row.attrib['stationID']
        stationname = row.attrib['stationName']
        solarsystem = row.attrib['solarSystemID']
        conq = {'name' : stationname, 'solarsystem': int(solarsystem), 'id': int(stationid)}
        conq_stations.append(conq)
        print "Conq: ",stationid,stationname,solarsystem
        import_station(db, stationid, stationname, solarsystem)
    with open('conq.json', 'w') as f:
        print >>f, json.dumps(conq_stations)
        
def import_stations_json(db):
    with open('stations.json', 'r') as f:
        stations = json.loads(f.read())
    for station in stations:
        print station
        import_station(db, station['id'], station['name'], station['solarsystem'])

def main(sqle = None, live_only = False, json = False):
    db = psycopg2.connect(database = 'evec', user = 'evec', port = 5432)
    if live_only:
        import_conq_stations(db)
    elif json:
        import_stations_json(db)
    else:
        dbs = sqlite3.connect(sqle)
        import_jumps(dbs, db)
        import_regions(dbs, db)
        import_systems(dbs, db)
        import_stations(dbs, db)


if __name__ == "__main__":

    if sys.argv[1] == "--live":
        main(live_only = True)
    elif sys.argv[1] == "--json":
        main(json = True)
    else:
        main(sys.argv[1])
    

