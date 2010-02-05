import md5
import string
import os
import Cheetah.Template

import cherrypy

from evecentral import display
from evecentral import evec_func

from evecentral.evec_func import EVCstate, emit_redirect
from evecentral.userlib import User,Corp


class Corps:

    @cherrypy.expose
    def edit_page(self, page, delete=None, create = None, set=None, contents = "", title = "", view = "public"):
        session = EVCstate(trust=True)
        db = evec_func.db_con()
        user = User.get(session, db)
        if not user.isdirector:
            return
        corp = Corp(db, user.corpid)
        cur = db.cursor()


        if delete and page != "index":
            cur.execute("DELETE FROM corppages WHERE pagename = %s AND corpid = %s AND pagename != 'index'",
                        [page, user.corpid])
            db.commit()
            page = 'index'
        if create and page:
            found = False
            try:
                page.index('.')
                found = True
            except:
                pass
            try:
                page.index('/')
                found = True
            except:
                pass
            try:
                page.index(' ')
                found = True
            except:
                pass

            if found:
                return evec_func.simple_error( "Invalid page name. No ., /, spaces")

            cur.execute("INSERT INTO corppages (pagename, corpid, title, contents, view) VALUES (%s, %s, 'New page', 'Type stuff here', 'public')",
                        [page, user.corpid])
            db.commit()


        if set:
            cur.execute("UPDATE corppages SET contents = %s, title = %s, view = %s, edit = NOW() WHERE pagename = %s AND corpid = %s",
                        [contents, title, view, page, user.corpid])
            db.commit()


        cur.execute("SELECT contents,title,view,edit FROM corppages WHERE corppages.corpid = %s AND corppages.pagename = %s",
                    [corp.corpid,page])
        r = cur.fetchone()


        if r:

            t = display.template('corpeditpage.tmpl', session)
            t.pcontents = r[0]
            t.ptitle = r[1]
            t.view = r[2]
            t.pagename = page
            t.pedit = r[3]
            t.corp = corp



        pages = []
        cur.execute("SELECT pagename FROM corppages WHERE corpid = %s", [user.corpid])
        r = cur.fetchone()
        while r:
            pages.append(r[0])
            r = cur.fetchone()
        t.pages = pages
        db.close()

        return t.respond()

    edit_page_html = edit_page

    @cherrypy.expose
    def view_page(self, ticker, page="index", retry = True):
        session = EVCstate(trust=True)
        db = evec_func.db_con()
        user = User.get(session, db)
        corp = None
        if user.valid:
            corp = Corp(db, user.corpid)
        cur = db.cursor()
        cur.execute("SELECT corps.corpid,contents,title,view,edit FROM corppages,corps WHERE corppages.corpid = corps.corpid AND corps.ticker = %s AND corppages.pagename = %s",
                    [ticker,page])
        r = cur.fetchone()


        if r:
            view = r[3]
            page_corp = Corp(db, long(r[0]))
            t = display.template('corpviewpage.tmpl', session)
            t.canedit = False
            print view
            if user.valid:
                if view == "corp only" and not long(r[0]) == corp.corpid and not user.ismember:
                    return evec_func.simple_error("You are not authorized to view this page")


                if view == "director only" and not long(r[0]) == corp.corpid and not user.isdirector:
                    return evec_func.simple_error("You are not authorized to view this page")


                t.canedit = user.isdirector and long(r[0]) == corp.corpid
            else:
                if view != "public":
                    return evec_func.simple_error("You are not authorized to view this page.")


            t.pcontents = r[1]
            t.ptitle = r[2]
            t.view = r[3]
            t.pagename = page
            t.pedit = r[4]
            t.corp = page_corp


            db.close()
            return t.respond()

        else:
            if retry:
                return self.view_page(ticker, page[:-5], retry = False)
            db.close()
            return evec_func.simple_error("No such page: " + ticker + " page " + page)

    view_page_html = view_page

    @cherrypy.expose
    def register(self):
        session = EVCstate(trust=True)
        db = evec_func.db_con()
        user = User.get(session, db)

        if user.valid is False:
            db.close()
            return evec_func.simple_error("Not logged in")

        if user.isdirector != 1:
            db.close()
            return evec_func.simple_error("Not director - only directors can do that")

        r = Corp.create(db, user.corpid, user.corporation)
        if r is False:
            db.close()
            return evec_func.simple_error("Corp exists")


        emit_redirect('/corps/')

        db.close()

    register_html = register

    @cherrypy.expose
    def manage(self, set = 0, description = "", join_password = "", headquarters = "", ticker = ""):
        session = EVCstate(trust=True)
        db = evec_func.db_con()
        user = User.get(session, db)


        if not user.valid:
            return evec_func.simple_error("Not logged in")
        if not user.isdirector:
            return evec_func.simple_error("Not enough priveleges")

        ucorp = Corp(db,user.corpid)
        if set:
            ucorp.description = description
            ucorp.headquarters = headquarters
            ucorp.join_password = join_password
            ucorp.ticker = ticker
            ucorp.update(db)

        t = display.template('corpmanage.tmpl', session)

        t.corp = ucorp

        return t.respond()

    manage_html = manage

    @cherrypy.expose
    def advertise(self):
        session = EVCstate(self, trust=True)
        db = evec_func.db_con()
        user = User.get(session, db)


        if not user.valid:
            return evec_func.simple_error("Not logged in")
        if not user.isdirector:
            return evec_func.simple_error("Not enough priveleges")

        ucorp = Corp(db,user.corpid)

        t = display.template('corpmanage.tmpl', session)

        t.corp = ucorp

        return t.respond()
    advertise_html = advertise

    @cherrypy.expose
    def index(self, join_password = None):
        session = EVCstate(trust=True)
        db = evec_func.db_con()
        user = User.get(session, db)


        t = display.template('corpmain.tmpl', session)

        ucorp = None

        if user.valid:
            ucorp = Corp(db,user.corpid)
            if join_password:
                if ucorp.join_password == join_password:
                    user.make_member(db)
                    session.save()




        t.can_create = False
        t.ucorp = ucorp
        if user.valid and (user.ismember or ucorp.join_password == ''):

            if user.isdirector:
                if not ucorp.exists:
                    t.can_create = True

            t.corp = user.corporation
            t.corpid = user.corpid
            t.ismember = 1
            t.user = user
        elif user.valid and not user.ismember:
            t.corp = user.corporation
            t.corpid = user.corpid
            t.user = user
            t.ismember = 0

        else:
            t.corp = None
            t.corpid = None
            t.can_create = False
            t.user = None
            t.ismember = 0



        cur = db.cursor()
        cur.execute("SELECT corpname,description,headquarters,ticker,corpid FROM corps WHERE ticker IS NOT NULL AND ticker != '' ORDER BY corpname")
        r = cur.fetchone()
        corps = []
        while r:
            c = {}
            c['corpname'] = r[0]
            c['description'] = r[1]
            c['headquarters'] = r[2]
            c['ticker'] = r[3]
            corpid = r[4]


            ccheck = db.cursor()
            ccheck.execute("SELECT contents FROM corppages WHERE corpid = %s", [corpid])
            rc = ccheck.fetchone()
            if rc[0] == "Placeholder text - this corporation has not yet provided a webpage":
                pass
            else:
                corps.append(c)
            r = cur.fetchone()

        t.corps = corps


        return t.respond()
    index_html = index
