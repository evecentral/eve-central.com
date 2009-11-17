#!/usr/bin/env python
import sys
sys.path.append('../lib/')
from evecentral import cache
import smtplib

def main():

    mailcount = int(cache.get("evec_mail_count"))
    mailsend = cache.get("evec_mail_sent")

    if mailsend is None:
        mailsend = 0
        cache.set("evec_mail_sent", "0")

    mailsend = int(mailsend)

    #print "Count",mailcount,"at",mailsend

    message = "price,volRemaining,typeID,range,orderID,volEntered,minVolume,bid,issued,duration,stationID,regionID,solarSystemID,jumps,source\n"
    #message = "CSV Header\n"

    if mailsend >= mailcount:
        return False

    thispass = 0
    cont = False # Continue?
    while mailsend < mailcount:
        mailsend += 1
        thispass += 1
        cache.incr("evec_mail_sent")
        try:
            message = message + cache.get("evec_mail_" + str(mailsend))
        except:
            pass
        if thispass > 50:
            cont = True
            break

    smtpsess = smtplib.SMTP('localhost')
    datae = "To: evec-upload@lists.stackworks.net\nPrecedence: bulk\nX-EVEC-UserIdHash: 0" + "\nSubject: Upload\n\n" + message
    smtpres = smtpsess.sendmail('uploader@stackworks.net', 'evec-upload@lists.stackworks.net', datae)
    return cont
    #print datae
    


if __name__ == '__main__':
    while main() == True:
        pass
