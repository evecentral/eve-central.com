import psycopg
import time
s = time.time()
print "Initialize"
import csv
import sys



db = psycopg.connect(database='evec', user='evec', host = 'localhost')

print "Import time: ",time.time()-s
s = time.time()

class ccp:
    delimiter = ','
    quotechar = '"'
    doublequote = True
    lineterminator = '\n'
    quoting = csv.QUOTE_MINIMAL
    skipinitialspace = False
    escapechar = None

ssid = set()

if True:

    f = open("dbo_mapSolarSystemJumps.csv")
    lines = f.readlines()[1:]


    ssid = set()


    print "Building edges..."
    cursor = db.cursor()
    csvread = csv.reader(lines, dialect = ccp)
    edge_file = open("edges_weight.csv", "w")
    for fields in csvread:
        fromss = long(fields[2])
        toss = long(fields[3])

        cursor.execute('SELECT security FROM systems WHERE systemid = %s', [fromss])
        fsec = float(cursor.fetchone()[0])
        cursor.execute('SELECT security FROM systems WHERE systemid = %s', [toss])
        tsec = float(cursor.fetchone()[0])
        weight = 1
        if tsec < 0.5 or fsec < 0.5:
            weight = 100


        print >> edge_file, fromss, toss, weight

        ssid.add(fromss)
        ssid.add(toss)



    edge_file.close()
    vertex_file = open("vertex.csv", "w")
    print "Building nodes..."
    for ss in ssid:
        print >> vertex_file, ss
