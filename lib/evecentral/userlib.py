import cherrypy

class Corp(object):
    def __init__(self, db, corpid):
        cur = db.cursor()
        self.exists = False
        self.join_password = ""
        self.ticker = ""
        if Corp.check_exist(db,corpid):
            cur.execute("SELECT corpname,description,headquarters,join_password,ticker FROM corps WHERE corpid = %s", [corpid])
            r = cur.fetchone()
            self.corporation = r[0]
            self.description = r[1]
            self.headquarters = r[2]
            self.join_password = r[3]
            self.ticker = r[4]
            self.exists = True
            self.corpid = corpid

    def create(db, corpid, corpname):

        if Corp.check_exist(db,corpid) is False:
            cur = db.cursor()
            cur.execute("INSERT INTO corps (corpid, corpname, description, headquarters) VALUES (%s,%s,%s,%s)",
                        [corpid, corpname, 'No description yet', 'Unspecified'])
            cur.execute("INSERT INTO corppages (corpid, pagename, contents, title) VALUES (%s,%s,%s,%s)",
                        [corpid, 'index', 'Placeholder text - this corporation has not yet provided a webpage',
                         "Welcome to "+corpname])
            db.commit()
            return True
        return False
    create = staticmethod(create)

    def update(self,db):
        cur = db.cursor()
        cur.execute("UPDATE corps SET description = %s, headquarters = %s, join_password = %s, ticker = %s WHERE corpid = %s",
                    [self.description, self.headquarters, self.join_password, self.ticker, self.corpid])
        db.commit()

    def check_exist(db, corpid):
        cur = db.cursor()
        cur.execute("SELECT corpname FROM corps WHERE corpid = %s", [corpid])
        r = cur.fetchone()
        if r:
            return True
        return False
    check_exist = staticmethod(check_exist)


class UserPreference(object):
    def __init__(self, user, preference, db):
        self.cur = db.cursor()
        self.user = user
        self.pref = preference

    def value(self):
        cur.execute("SELECT value FROM user_pref WHERE userid = %s AND preference = %s", [self.user.userid, self.preference])
        r = cur.fetchone()
        if r is None:
            return None
        else:
            return r[0]

    def set(self, value):
        cur.begin()
        cur.execute("DELETE FROM user_pref WHERE userid = %s AND preference = %s", [self.user.userid, self.preference])
        cur.execute("INSERT INTO user_pref (userid, preference, value) VALUES (%s, %s, %s)", [self.user.userid, self.preference, str(value)])
        cur.commit()
        return value
    

class User(object):
    def get(session, db):

        if 'user' in session:
            u = session['user']
            u.update_user(db)
            return u
        else:
            return User(session, db)
    get = staticmethod(get)

    def salt(password, username):
        return password + "This is a boring salt, and not very good - " + username
    salt = staticmethod(salt)

    
    def __init__(self, session, db):
        self.valid = False


        # Look at session user info
        if session and 'user' not in session:
            pass
        if session and 'user' in session:
            try:
                self.update_user(db)
            except:
                pass

    def get_email(self, db, username):
        cur = db.cursor()
        cur.execute("SELECT email FROM users WHERE username = %s", [username])
        try:
            r = cur.fetchone()
            return r[0]
        except:
            return False


    def make_member(self, db):
        cur = db.cursor()
        cur.execute("UPDATE users SET ismember = 1 WHERE userid = %s", [self.userid])
        db.commit()
        self.ismember = 1

    def change_pw_name(self, db, username, newpw):
        cur = db.cursor()
        cur.execute("UPDATE users SET password = md5(%s) WHERE  username = %s", [User.salt(newpw, username), username])
        db.commit()
        self.password = newpw

    def change_pw(self,db,oldpw,newpw):
        cur = db.cursor()
        cur.execute("UPDATE users SET password = md5(%s) WHERE  password = md5(%s) AND userid = %s", [User.salt(newpw, self.username), User.salt(oldpw, self.username), self.userid])
        db.commit()
        self.password = newpw

    def login(db, session, username, password, ignorepw = False): #STATIC
        cur = db.cursor()
        self = object()

        cur.execute("SELECT userid, corporation, corpid, isdirector, ismember, full_apikey, apiuserid, evecpoints, api_cacheuntil FROM users WHERE username = %s AND password = md5(%s)",
                    [username, User.salt(password, username)])

        r = cur.fetchone()
        self = User(session, db)
        if r:
            self.username = username
            self.userid = long(r[0])
            self.corporation = r[1]
            self.corpid = long(r[2])
            self.isdirector = r[3]
            self.ismember = r[4]
            self.full_apikey = r[5]
            self.apiuserid = r[6]
            self.evecpoints = r[7]
            self.api_cacheuntil = r[8] # these shouldn't really be here
            
            self.valid = True
            session['user'] = self
            return self
        else:
            return False # Login failed
    login = staticmethod(login) # STATIC


    def fakelogin(db, username):
        cur = db.cursor()
        self = object()

        cur.execute("SELECT userid, corporation, corpid, isdirector, ismember, full_apikey, apiuserid, evecpoints, api_cacheuntil FROM users WHERE username = %s",
                    [username])

        r = cur.fetchone()
        self = User(None, db)
        if r:
            self.username = username
            self.userid = long(r[0])
            self.corporation = r[1]
            self.corpid = long(r[2])
            self.isdirector = r[3]
            self.ismember = r[4]
            self.full_apikey = r[5]
            self.apiuserid = r[6]
            self.evecpoints = r[7]
            self.api_cacheuntil = r[8]
            
            self.valid = True
            return self
        else:
            return False # Login failed
    fakelogin = staticmethod(fakelogin) # STATIC


    def register(db, password, email): # LOGIN
        headers = cherrypy.request.headers

        if 'Eve.Charid' in headers and not User.user_exists(db, long(headers['Eve.Charid'])):
            # This really really really really doesn't belong here....
            cur = db.cursor()
            userid = long(headers['Eve.Charid'])
            username = headers['Eve.Charname']
            corpid = long(headers['Eve.Corpid'])
            corporation = headers['Eve.Corpname']
            alliance = headers['Eve.Alliancename']
            flags = long(headers['Eve.Corprole'])
            isdirector = 0
            if flags & (2**0) == 1:
                isdirector = 1
            cur.execute("INSERT INTO users (userid, username, password, email, corporation, alliance, isdirector, corpid, ismember) VALUES (%s,%s,md5(%s),%s,%s,%s,%s,%s,%s)",
                        [userid,username,
                         User.salt(password, username), email,
                         corporation,
                         alliance,
                         isdirector,
                         corpid,
                         0]) # is member is default 0
            db.commit()
            return True
        else:
            return False
    register = staticmethod(register) # STATIC

    def update_user(self, db):
        headers = cherrypy.request.headers
        cur = None
        if 'Eve.Charid' in headers and User.user_exists(db, long(headers['Eve.Charid'])):
            cur = db.cursor()
            corp = Corp(db,self.corpid)
            if corp.join_password == "":
                self.ismember = 1
                
            # This really really doesn't belong here either - a domain class crusing through request headers? wtf? :)
            userid = long(headers['Eve.Charid'])
            username = headers['Eve.Charname']
            corpid = long(headers['Eve.Corpid'])
            corporation = headers['Eve.Corpname']
            alliance = headers['Eve.Alliancename']
            flags = long(headers['Eve.Corprole'])



            # Attempt to pick up corp changes and see if membership gets kicked
            ncorp = Corp(db,corpid)
            if long(self.corpid) != corpid:
                if ncorp.join_password != "":
                    self.ismember = 0


            self.corpid = corpid


            isdirector = 0
            if flags & (2**0) == 1:
                isdirector = 1
            cur.execute("UPDATE users SET ismember = %s, username = %s, corporation = %s, alliance = %s, isdirector = %s, corpid = %s WHERE userid = %s",
                        [self.ismember,
                         username,
                         corporation,
                         alliance,
                         isdirector,
                         corpid,userid])
            corp = Corp(db, corpid)

            self.corporation = corporation
            self.corpid = corpid

            self.isdirector = isdirector
            db.commit()
        cur = db.cursor()

        cur.execute("UPDATE users SET full_apikey = %s, apiuserid = %s, api_cacheuntil = %s WHERE userid = %s",
                        [self.full_apikey,
                         self.apiuserid,
                         str(self.api_cacheuntil),
                         self.userid])

        db.commit()
    def user_exists(db, userid):
        cursor = db.cursor()
        cursor.execute("SELECT username FROM users WHERE userid = %s", [userid])
        if cursor.fetchone():
            return True
        else:
            return False
    user_exists = staticmethod(user_exists)
