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


from numpy import *
import cmemcache as memcache
import hashlib

from evecentral import evec_func
from evecentral import cache

CACHE_TIME = 3600


def cache_name(typeid, hours, sql_system, regionlimit, buysell, minQ):
    regions = ""
    for region in regionlimit:
        regions += "-" + str(region)
    name =  "evec_stats_" + str(typeid) + str(hours) + str(sql_system) + str(regions) + str(buysell) + str(minQ)
    nsname = "evec_stats_" + hashlib.md5(name).hexdigest()
    return nsname

def sum_volumes(volenter, volremain):
    ea = array(volenter, dtype=int)
    er = array(volremain, dtype=int)

    enter = ea.sum()
    remain = er.sum()
    move = enter - remain
    return (enter,remain,move)


def calculate_stats(list, weight = None):
    if len(list) == 0:
        return (0,0,0,0,0,0)


    sarray = array(list, dtype=float)
    warray = None
    if weight is None:
        warray = array(ones(len(sarray)), dtype = float)
    else:
        warray = array(weight, dtype=float)

    med = median(sarray)

    avg = average(sarray, weights = warray)

    st = std(sarray)
    va = var(sarray)
    maxv = sarray.max()
    minv = sarray.min()


    return (med,avg,st,va, maxv, minv)


def item_stat(db, typeid, hours = 48, sql_system = " ", regionlimit = [], buysell = True, minQ = 0):
    global CACHE_TIME
    obj_name = cache_name(typeid, hours, sql_system, regionlimit, buysell, minQ)
    
    cache_obj = cache.get(obj_name)
    if cache_obj:
        return cache_obj
    
    sql_age = `hours`+" hours"
    reg_block_stat = evec_func.build_regionquery("current_market", regionlimit)
    stat = db.cursor()

    query_string = "SELECT price,bid,volremain,volenter FROM current_market WHERE " + reg_block_stat + " AND typeid = %s AND price > 0.15 AND volenter >= %s AND age(reportedtime) < '" + sql_age + "'" + sql_system

    sell = {}
    buy = {}

    median_price_sell = []
    volenter_sell = []
    volremain_sell = []

    median_price_buy = []
    volenter_buy = []
    volremain_buy = []

    stat.execute(query_string , [typeid, minQ])

    r = stat.fetchone()
    while r:
        if r[1] == 1:
            median_price_buy.append(r[0])
            volenter_buy.append(r[3])
            volremain_buy.append(r[2])
        else:
            median_price_sell.append(r[0])
            volenter_sell.append(r[3])
            volremain_sell.append(r[2])

        r = stat.fetchone()

    if buysell:

        res_s = calculate_stats(median_price_sell, volenter_sell)
        res_b = calculate_stats(median_price_buy, volenter_buy)
        vol_s = sum_volumes(volenter_sell, volremain_sell)
        vol_b = sum_volumes(volenter_buy, volremain_buy)

        for (r, v, emap) in [(res_s,vol_s,sell), (res_b,vol_b,buy)]:
            emap['median'] = r[0]
            emap['avg_price'] = r[1]
            emap['total_vol'] = v[0]
            emap['total_movement'] = v[2]
            emap['stddev'] = r[2]
            emap['max'] = r[4]
            emap['min'] = r[5]

        cache.set(obj_name, (sell, buy), CACHE_TIME)
        return (sell,buy)

    else:
        median_price_sell.extend(median_price_buy)
        volenter_sell.extend(volenter_buy)
        volremain_sell.extend(volremain_buy)

        r = calculate_stats(median_price_sell)
        v = sum_volumes(volenter_sell, volremain_sell)
	try:
            sell['median'] = r[0]
	    sell['avg_price'] = r[1]
	    sell['total_vol'] = v[0]
	    sell['total_movement'] = v[2]
	    sell['stddev'] = r[2]
	    sell['max'] = r[4]
	    sell['min'] = r[5]
	except:
	    sell['avg_price'] = 0
	    sell['total_vol'] = 0
	    sell['total_movement'] = 0
	    sell['stddev'] = 0
	    sell['max'] = 0
	    sell['min'] = 0

        cache.set(obj_name, sell, CACHE_TIME)
	return sell
