import matplotlib
matplotlib.use("Agg")

import pylab
import matplotlib.text
import matplotlib.dates

import psycopg
import sys
from mx.DateTime import *
import datetime
import pickle
from numpy import *
sys.path.append('../lib/')

from evecentral import display

#import evec_func
db = psycopg.connect(database='evec', user='evec', host = 'localhost', port = '9999')


def get_baskets():
    global db
    cur = db.cursor()
    cur.execute('SELECT DISTINCT ON (basket) basket FROM basket')
    list = cur.fetchall()
    ret = []
    for l in list:
	ret.append(l[0])
    return ret

def get_basket_contents(basket):
    global db
    cur = db.cursor()
    cur.execute('SELECT basket.typeid,basket.weight,types.typename FROM basket,types WHERE basket = %s AND basket.typeid = types.typeid' , [basket])
    return cur.fetchall()


def get_stats(typeid,date):
    global db
    cur = db.cursor()
    cur.execute("SELECT AVG(price),SUM(volremain),SUM(volenter) - SUM(volremain),bid FROM archive_market WHERE typeid = %s AND (reportedtime) :: date = %s GROUP BY orderid,bid", [typeid, date])
    a = cur.fetchall()
    avg_b = array(zeros(len(a)),dtype=float)
    vol_b = array(zeros(len(a)),dtype=float)
    move_b = array(zeros(len(a)),dtype=float)
    avg_s = array(zeros(len(a)),dtype=float)
    vol_s = array(zeros(len(a)),dtype=float)
    move_s = array(zeros(len(a)),dtype=float)

    x_s = 0
    x_b = 0
    for r in a:
	if r[3]:
	    avg_b[x_b] = r[0]

	    vol_b[x_b] = r[1]
	    move_b[x_b] = r[2]
	    x_b += 1
	else:
	    avg_s[x_s] = r[0]
	    vol_s[x_s] = r[1]
	    move_s[x_s] = r[2]
	    x_s += 1
    avg_b.resize(x_b)
    avg_s.resize(x_s)
    vol_b.resize(x_b)
    vol_s.resize(x_s)
    move_b.resize(x_b)
    move_s.resize(x_s)
    b = (None,None,None)
    s = (None,None,None)
    try:
	b = (pylab.median(avg_b), pylab.mean(vol_b), pylab.mean(move_b))
	s = (pylab.median(avg_s), pylab.mean(vol_s), pylab.mean(move_s))
    except:
	return (b,b,b)

    ret = ( ((b[0]+s[0])/2, (b[1]+s[1])/2, (b[2]+s[2])/2), b, s)
    print ret
    return ret







def do_basket_date(basket,date):
    contents =	get_basket_contents(basket)
    ret = {}
    print date
    for (typeid,weight,name) in contents:
	r = get_stats(typeid,date)
	ret[typeid] = r
    return ret


def do_basket(data, basket):

    date = Date(2008,1,15)


    while date < now() - RelativeDateTime(days = 1):
#	 print "Working on",date.date
	if date not in data.keys():
	    r = do_basket_date(basket,date.date)
	    data[date] = r
	date += RelativeDateTime(days=1)




def get_weight(contents, typeid):
    for (type,weight,name) in contents:
	if type == typeid:
	    return weight
    return 1


def build_moving5(days, avg):
    moving5 = array(zeros(len(days)-4), dtype = float)
    cday = 1
    moving5[0] = pylab.mean(avg[0:4])
    for a in avg[5:]:
	moving5[cday] = pylab.mean(avg[cday:cday+4])
	cday += 1
    return moving5



def plot_basket(basket_data, basket, dayrange=60, domoving=True, dobuysell=True, sellonly=False):
    days = basket_data['byday'].keys()
    days.sort()
    days = days[-dayrange:]


    fdays = []

    avg = array(zeros(len(days)), dtype=float)
    avg_s = array(zeros(len(days)), dtype=float)
    avg_b = array(zeros(len(days)), dtype=float)

    cday = 0

    for day in days:
	n = len(basket_data['byday'][day].keys())
	n += 0.0
	s = 0.0
	s_s = 0.0
	s_b = 0.0

	bail_today = False

	for type in basket_data['byday'][day].keys():
	    weight = get_weight(basket_data['contents'], type)

	    try:
		s += basket_data['byday'][day][type][0][0] * weight
		s_b += basket_data['byday'][day][type][1][0] * weight
		s_s += basket_data['byday'][day][type][2][0] * weight
	    except:
		bail_today = True


	fdays.append(datetime.date(day.year, day.month, day.day))


	if bail_today or s < 0.05 or s_s < 0.05 or s_b < 0.05:
	    if cday == 0:
		avg[cday] = 0
		avg_s[cday] = 0
		avg_b[cday] = 0
	    else:

		avg[cday] = avg[cday-1]
		avg_s[cday] = avg_s[cday-1]
		avg_b[cday] = avg_b[cday-1]
	    cday += 1
	    continue



	avg[cday] = s/n
	avg_s[cday] = s_s/n
	avg_b[cday] = s_b/n




	cday += 1



    if domoving:
	moving5 = build_moving5(days, avg)
	moving5_b = build_moving5(days, avg_b)
	moving5_s = build_moving5(days, avg_s)


    fdays = matplotlib.dates.date2num(fdays)



    ax = pylab.subplot(111)

    if not sellonly:
        pylab.plot_date(fdays,avg,'b-')


    if dobuysell and not sellonly:
	pylab.plot_date(fdays,avg_s,'c-')
	pylab.plot_date(fdays,avg_b,'r-')

    if sellonly:
        pylab.plot_date(fdays,avg_s,'c-')
        pylab.plot_date(fdays[4:],moving5_s,'g--')

    if domoving and not sellonly:
	pylab.plot_date(fdays[4:],moving5,'g-')
	if dobuysell:
	    pylab.plot_date(fdays[4:],moving5_s,'g--')
	    pylab.plot_date(fdays[4:],moving5_b,'g--')

    

    ymin = 0
    ymax = 0
    if dobuysell and not sellonly:
	ymin = min([min(avg_s),min(avg), min(avg_b)])
	ymax = max([max(avg), max(avg_s), max(avg_b)])
    elif not sellonly:
	ymin = min(avg)
	ymax = max(avg)
    elif sellonly:

        ymin = min(avg_s)
        ymax = max(avg_s)
        print "Sellonly",ymin,ymax


    interval = None
    if dayrange == 60:
	interval = 12
    elif dayrange == 180:
	interval = 36
    else:
	interval = 6

    ax.xaxis.set_major_locator(matplotlib.dates.DayLocator(interval=interval))
    ax.xaxis.set_minor_locator(matplotlib.dates.DayLocator(interval=1))
    ax.xaxis.set_major_formatter(matplotlib.dates.DateFormatter('%D'))
    ax.autoscale_view()
    pylab.grid(True)
    pylab.ylim(ymin-(ymin/32),ymax+(ymax/32))
    pylab.ylabel("Index Price (ISK)")
    pylab.title("Market Index: "+basket+", Past " +`dayrange`+" days")

    other = ""

    if sellonly:
        other = other + "-sellonly"
    else:
        if not dobuysell:
            other = other + "-nobuysell"
        if not domoving:
            other = other + "-nomoving"
    try:
        pylab.savefig('/www/eve-central.com/static_web/reports/'+basket+'-'+`dayrange`+other+'.png')
    except:
        print "Can't save figure... skipping for now"
    pylab.close()






cur = db.cursor()




baskets = get_baskets()
baskets.reverse()

for basket in baskets:
    basket_data = {}
    print basket
    try:
	f = open(basket+'.pickle', 'r')
	basket_data = pickle.load(f)
	f.close()
	basket_data['contents'] = get_basket_contents(basket)
    except:
	basket_data['contents'] = get_basket_contents(basket)
	basket_data['byday'] = {}


    do_basket(basket_data['byday'], basket)
    f = open(basket+'.pickle', 'w')
    pickle.dump(basket_data,f)
    f.close()


    # Generate basket html

    session = dict()
    t = display.template('report.tmpl', session)

    for dr in [30,60,180]:
        
        for sellonly in [True,False]:
            
            plot_basket(basket_data, basket, dayrange = dr, domoving=True, sellonly = sellonly)

	for moving in [True,False]:
	    for buysell in [True,False]:
		plot_basket(basket_data, basket, dayrange = dr, domoving=moving, dobuysell = buysell)

    t.basket = basket

    t.contents = basket_data['contents']
    t.last_generated = now()
    t.data_upto = now() -RelativeDateTime(days=1)
    f = open('/www/eve-central.com/static_web/reports/'+basket+".html", "w")
    print >> f, t.respond()
    f.close()
