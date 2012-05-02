package com.evecentral.mail

import akka.actor.Actor
import org.slf4j.LoggerFactory
import com.evecentral.datainput.UploadCsvRow

class MailDispatchActor extends Actor {

  import akka.actor.Scheduler
  import java.util.concurrent.TimeUnit
  import javax.mail.internet.{InternetAddress, MimeMessage}
  import java.util.{Date, Properties}
  import javax.mail._

  private val log = LoggerFactory.getLogger(getClass)


  case class SendNow()

  override def preStart() {
    log.info("Starting the mail service schedule")
    Scheduler.schedule(self, SendNow, 30, 5 * 60, TimeUnit.SECONDS)
  }

  private val sendRows = new scala.collection.mutable.Queue[UploadCsvRow]()

  private def sendEmailNow {
    log.info("Starting mail dispatch")
    try {
      val props = new Properties();
      props.put("mail.smtp.host", "localhost");
      props.put("mail.debug", "false");
      val session = Session.getInstance(props);
      val msg = new MimeMessage(session)
      msg.setFrom(new InternetAddress("uploader@stackworks.net"))
      val address = Array[javax.mail.Address](new InternetAddress("evec-upload@lists.stackworks.net"));
      msg.setRecipients(Message.RecipientType.TO, address);
      msg.setSubject("Upload");
      msg.setSentDate(new Date());

      val text = "price,volRemaining,typeID,range,orderID,volEntered,minVolume,bid,issued,duration,stationID,regionID,solarSystemID,jumps,source,generatedAt\n" + sendRows.mkString("\n")
      msg.setText(text)
      Transport.send(msg)
      sendRows.clear()
    } catch {
      case mex: MessagingException =>
        log.error("Can't send message: ", mex)
    }
  }

  def receive = {
    case data: Seq[UploadCsvRow] => { sendRows++= data; log.info("Scheduling mail - " + sendRows.size + " entries"); }
    case SendNow => { if (sendRows.nonEmpty) sendEmailNow else log.info("No mail to send") }
    case _ => log.error("mail dispatch didn't know what to do - wrong type")
  }
}
