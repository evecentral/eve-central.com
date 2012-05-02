package com.evecentral.datainput

import com.evecentral.mail.MailDispatchActor
import akka.actor.Actor
import Actor._
import cc.spray.typeconversion.DefaultMarshallers
import cc.spray.Directives
import com.evecentral.dataaccess.StaticProvider
import com.evecentral.{Database, PoisonCache, OrderCacheActor, ECActorPool}
import org.slf4j.LoggerFactory

class OldUploadServiceActor extends ECActorPool {

  private val log = LoggerFactory.getLogger(getClass)

  def mailActor = {
    val r = Actor.registry.actorsFor[MailDispatchActor]
    r(0)
  }

  def instance = actorOf(new Actor with DefaultMarshallers with Directives {

    def insertData(marketType: Int, regionId: Long, rows: Seq[UploadCsvRow]) {
      import net.noerd.prequel.SQLFormatterImplicits._

      Database.coreDb.transaction {
        tx =>
          tx.executeBatch("INSERT INTO archive_market (regionid, systemid, stationid, typeid,bid,price, orderid, minvolume, volremain, " +
            "volenter, issued, duration, range, reportedby, source)" +
            "VALUES( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 'evec_upload_cache')") {
            statement =>
              rows.foreach { row =>
                statement.executeWith(row.regionId, row.solarSystemId, row.stationId, row.marketTypeId, if(row.bid) 1 else 0, row.price,
                  row.orderId, row.minVolume, row.volRemain, row.volEntered,
                  row.issued, row.duration.toString + " days", row.range)
              }
          }

          tx.execute("DELETE FROM current_market WHERE regionid = ? AND typeid = ?", regionId, marketType.toLong)

          tx.executeBatch("INSERT INTO current_market (regionid, systemid, stationid, typeid,"+
            "bid,price, orderid, minvolume, volremain, volenter, issued, duration, range, reportedby)" +
          "VALUES( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)") {
            statement =>
              rows.foreach { row =>
                statement.executeWith(row.regionId, row.solarSystemId, row.stationId, row.marketTypeId, if (row.bid) 1 else 0, row.price, row.orderId, row.minVolume, row.volRemain,
                row.volEntered, row.issued, row.duration.toString + " days", row.range, 0)
              }
          }
      }
    }
    
    def poisonCache(marketType: Int, regionId: Long) {
      val a = Actor.registry.actorsFor[OrderCacheActor]
      (a(0) ? PoisonCache(StaticProvider.regionsMap(regionId), StaticProvider.typesMap(marketType))).await

    }
    
    def procData(rows: Seq[UploadCsvRow]) {
      val regionType = rows.map(row => (row.marketTypeId, row.regionId)).distinct
      if (regionType.length == 1) {
        val regionId = regionType(0)._2
        val typeId = regionType(0)._1
        mailActor ! rows
        insertData(typeId, regionId, rows)
        poisonCache(typeId, regionId)
      }
    }

    def receive = {
      case OldUploadPayload(ctx, typename, userid, data, typeid, region) => {
        log.info("Processing upload payload for " + typeid)
        val lines = data.split("\n").tail
        val rows = lines.map(UploadCsvRow(_))
        if (rows.nonEmpty)
          procData(rows)
        ctx.complete("Ok!")
      }
    }
  })
}
