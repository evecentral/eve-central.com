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

#import cmemcache as memcache
import memcache
import copy
import hashlib

DEFAULT_EXPIRE = 3600

_hits = 0
_miss = 0
_last_key_s = ""
_last_key_f = ""

def hits():
    global _hits
    return _hits
def miss():
    global _miss
    return _miss

def last_key_s():
    global _last_key_s
    return _last_key_s
def last_key_f():
    global _last_key_f
    return _last_key_f

mc = memcache.Client(['127.0.0.1:11211'], debug = 0)    

def set(key, data, expire = None):
    global DEFAULT_EXPIRE, _last_key_s, mc

    if expire is None:
        expire = DEFAULT_EXPIRE
    _last_key_s = key
    r = mc.set(key, data, expire)

    return r

def get(key):
    global _miss,_hits, _last_key_f, mc

    _last_key_f = key
    v = mc.get(key)

    if v is None:
        _miss += 1
    else:
        _hits += 1

    return v

def incr(key):
    global mc
    r = mc.incr(key)
    if r is None:
        mc.set(key, "0")
        return 0
    else:
        return int(r)

def decr(key):
    global mc
    return mc.decr(key)



def generic_key(prefix, *args):
    name = prefix + "_"
    for arg in args: # Sort lists
        if isinstance(arg, list):
            nl = copy.copy(arg)
            nl.sort()
            name += str(nl)
        else:
            name += str(arg)
    # Remove spaces for memcache use
    nsname = prefix + "_" + hashlib.md5(name).hexdigest()
    return nsname
            
