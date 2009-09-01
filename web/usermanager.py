import md5
import string
import os
import Cheetah.Template

from evecentral import display
from evecentral import evec_func
import random
import smtplib


import cherrypy

from evecentral.evec_func import EVCstate, emit_redirect
from evecentral.userlib import User


class Users:

    @cherrypy.expose
    def passreset(self, username, uemail):
        session = EVCstate(trust=True)
        db = evec_func.db_con()


        u = User.get(session, db)
        email = u.get_email(db, username)


        if email is False:
            r = "<html><head><title>Password reset failed</title></head><body>"
            r+= "Password reset failed. Please check your email address. "
            r += "</body></html>"
            db.close()
            return r

        if uemail.lower() != email.lower():

            r = "<html><head><title>Email not match</title></head>"
            r = "<body>The email address provided does not match the one on file - process aborted.</body></html>"
            db.close()
            return r

        if email:
            newpass = str(random.randint(10000,9000000))
            msg = "Subject: EVE-Central.com Password Reset\nTo: " + email + "\nFrom: EVE-Central.com <atrus@stackworks.net>\n\nThe password for username " + username + " has been reset to " + newpass
            u.change_pw_name(db, username, newpass)
            server = smtplib.SMTP('localhost')
            server.sendmail("atrus@stackworks.net", email, msg)

        r = "<html><head><title>Password reset</title></head><body>"
        r += "Password reset mail sent. Please check your inbox. Go to <a href=/users/>user home</a>"
        r += "</body></html>"

        db.close()
        return r


    passreset_html = passreset

    @cherrypy.expose
    def login(self, username, password):
        session = EVCstate(trust=True)
        db = evec_func.db_con()

        res = None

        r = User.login(db, session, username, password)
        if r is not False:

            if 'isigb' not in session or not session['isigb']:
                emit_redirect('/users/index.html')


            res = "<html><head><title>Logged in</title></head><body>"
            res += "Logged in! Go to <a href=/users/index.html>user home</a>. You are getting this page because the IGB does not know how to redirect."
            res += "</body></html>"
            session['user'] = r


        else:

            res = "<html><head><title>Login failed</title></head><body>"
            res += "Your login failed due to a bad password or username."
            res += "<form method=GET action=/users/passreset.html>Send a reset email for the user " + username
            res += " to the email address <input type=text name=uemail> (must match email on file!)"
            res += "<input type=hidden name=username value=\""+username+"\"><input type=submit value=Send>"
            res += "</form>"
            res += "</body></html>"



        db.close()
        return res

    login_html = login

    @cherrypy.expose
    def changepw(self, oldpw, newpw, newpw2):
        session = EVCstate(trust=True)
        db = evec_func.db_con()


        if newpw != newpw2:
            return evec_func.simple_error("Passwords do not match")


        u = User.get(session, db)
        u.change_pw(db, oldpw,newpw)
        emit_redirect('/users/')


        db.close()

    changepw_html = changepw

    @cherrypy.expose
    def logout(self):
        session = EVCstate(trust=True)
        db = evec_func.db_con()

        try:
            session['user'] = None
            del session['user']

        except:
            pass
        return emit_redirect('/')

    logout_html = logout

    @cherrypy.expose
    def register(self, password,email):
        session = EVCstate(trust=True)


        charname = None
        if 'Eve.Charname' in dict(cherrypy.request.headers):
            charname = cherrypy.request.headers['Eve.Charname']

        if charname is None:
            return evec_func.simple_error("No username found?")

        if password == "":
            return evec_func.simple_error("Please specify a password")


        if '@' not in email:
            return evec_func.simple_error("Please specify a semi-valid email address")


        db = evec_func.db_con()
        password = password.strip()
        r = User.register(db, password,email)
        if r is False:
            db.close()
            return evec_func.simple_error("Error: Registration error. You may already be registered or the system messed up")



        User.login(db, session, charname, password)
        emit_redirect('/users/')
        return """<html><head><title>Hi</title></head><body>
        Registered! Go to <a href=/users/index.html>user home</a>. The IGB knows nothing about redirecting since you're seeing this.
        </body></html>"""

    register_html = register


    @cherrypy.expose
    def setapikeys(self, full_apikey = None, apiuserid = None, error = 0):
        session = EVCstate(trust=True)
        db = evec_func.db_con()
        user = User.get(session, db)

        if not user.valid:
            return

        t = display.template('user_setapikeys.tmpl', session)

        t.errormsg = ""
        t.full_apikey = user.full_apikey
        t.apiuserid = user.apiuserid

        if full_apikey is not None and full_apikey != "":
            user.full_apikey = full_apikey
            user.apiuserid = apiuserid
            session['user'] = user
            user.update_user(db)
            session.save()
        t.full_apikey = user.full_apikey
        t.apiuserid = user.apiuserid

        if error == 1:
            t.errormsg = "We couldn't access the API services with the keys below - please double check your input"

        return t.respond()

    setapikeys_html = setapikeys


    @cherrypy.expose
    def index(self, message=""):
        session = EVCstate(trust=True)
        db = evec_func.db_con()
        user = User.get(session, db)


        if user.valid is False:
            t = display.template('registerlogin.tmpl', session)
            if 'Eve.Charname' in dict(cherrypy.request.headers):
                t.charname = cherrypy.request.headers['Eve.Charname']
            else:
                t.charname = ""
        else:
            t = display.template('usermain.tmpl', session)
            t.charname = user.username

        t.message = message
        t.user = user
        hdump = ""
        for name in dict(cherrypy.request.headers):
            hdump = hdump + name + ":" + cherrypy.request.headers[name] + "<br>"

        return t.respond() + hdump

    index_html = index
