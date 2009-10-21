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

import cmemcache as memcache
import copy

DEFAULT_EXPIRE = 3600

def nulllog(*args):
    pass



mc = memcache.Client(['127.0.0.1:1121'], debug = 0)
mc.log = nulllog


def set(key, data, expire = None):
    if expire is None:
        expire = DEFAULT_EXPIRE
    return mc.set(key, data, expire)

def get(key):
    return mc.get(key)
    


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
    nsname = name.replace(' ', '_')
    return nsname
            
