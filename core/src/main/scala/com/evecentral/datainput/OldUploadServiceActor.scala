package com.evecentral.datainput

import com.evecentral.ECActorPool
import com.evecentral.mail.MailDispatchActor
import akka.actor.Actor
import Actor._
import cc.spray.typeconversion.DefaultMarshallers
import cc.spray.Directives

class OldUploadServiceActor extends ECActorPool {

  def mailActor = {
    val r = Actor.registry.actorsFor[MailDispatchActor]
    r(0)
  }

  def instance = actorOf(new Actor with DefaultMarshallers with Directives {

    def procData(rows: Seq[UploadCsvRow]) {
      mailActor ! rows

    }

    def receive = {
      case OldUploadPayload(ctx, typename, userid, data, typeid, region) => {
        val lines = data.split("\n").tail
        val rows = lines.map(UploadCsvRow(_))
        if (rows.nonEmpty)
          procData(rows)
        ctx.complete("Ok!")
      }
    }
  })
}
