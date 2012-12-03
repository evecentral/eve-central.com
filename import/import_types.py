import psycopg2
import csv

db = psycopg2.connect(database = 'evec', user = 'evec', port = 9999)
cur = db.cursor()

stat = open("types.txt")

lines = stat.readlines()

#del lines[0]
#class sqldump(csv.excel):
#    delimiter = ','
#    quotechar = '"'
#    escapechar = None
#    doublequote = True
#    skipinitialspace = False
#    quoting = csv.QUOTE_MINIMAL

# select typeID,groupID,typeName,description,iconID,0,mass,volume,capacity,portionSize,raceID,basePrice,published,marketGroupID, chanceOfDuplicating from invTypes
# select typeID,groupID,typeName,description,iconID,0,mass,volume,capacity,1,raceID,1,published,marketGroupID,0 from invTypes;
#typeID,groupID,typeName,description,graphicID,radius,mass,volume,capacity,portionSize,raceID,basePrice,published,marketGroupID,chanceOfDuplicating
#0    1   2     3        4     5   6     7      8        9       10    11     12        13        14

csvread = csv.reader(lines)
for fields in csvread:

    print fields
    id = long(fields[0])

    name = fields[2].strip()

    published = 0

    description = fields[3].strip()
    volume = float(fields[7])
    if volume == 0:
        volume = 0.001


    published = "1"
    try:
        published = int(fields[12])
    except:
        pass

    marketgroup = fields[13]
    # Skip non-market items
    if marketgroup == "null" or marketgroup == "" or marketgroup == "0.0" or marketgroup == "0":
        print "SKIP: ",name,volume,published,marketgroup,id
        continue
    marketgroup = int(marketgroup)

    group = int(fields[1])
    print name,volume,published,group,marketgroup,id

    try:
        cur.execute("UPDATE types SET typename = %s, published = %s, groupid = %s, marketgroup = %s WHERE typeid = %s", [name, published, group, marketgroup, id])
        db.commit()
    except:
        db.rollback()



    try:
        cur.execute("INSERT INTO types (typeid, typename, size, published, groupid, marketgroup) VALUES (%s,%s,%s, %s, %s, %s)", [id, name, volume, published, group, marketgroup])
        print "Added new type ",id,name
        db.commit()
    except Exception,e:
        db.rollback()



db.commit()
cur.execute("UPDATE types SET size = 1.1 WHERE size is NULL")
db.commit()
