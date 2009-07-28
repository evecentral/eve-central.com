import random

def upload_suggest(db, region, rettype = "names"):

    cur = db.cursor()
    cur.execute("SELECT DISTINCT ON(types.typename) types.typename,types.typeid FROM types, current_market WHERE types.typeid = current_market.typeid AND current_market.regionid = %s AND current_market.reportedtime < (NOW() - interval '2 days') AND current_market.reportedtime > (NOW() - interval '12 days') GROUP BY types.typename,types.typeid,current_market.reportedtime LIMIT 200 OFFSET 0", [region])
    l = []

    r = cur.fetchone()
    while r:
        if rettype == "names":
            l.append(r[0])
        elif rettype == "ids":
            l.append(r[1])
        else:
            tup = (r[0], r[1])
            l.append(tup)

        r = cur.fetchone()
    random.shuffle(l)

    return l[0:20]
