import Cheetah.Template


def template(file,session):
    t = None
    try:
        t = Cheetah.Template.Template(file = '/www/eve-central.com/cherry/templates/' + file)
    except:
        t = Cheetah.Template.Template(file = 'templates/' + file)

    try:
        t.isigb = session['isigb']
    except:
        t.isigb = False


    t.session = session
    return t
