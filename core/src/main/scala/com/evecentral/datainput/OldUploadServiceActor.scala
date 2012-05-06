package com.evecentral.datainput

import com.evecentral.mail.MailDispatchActor
import akka.actor.Actor
import Actor._
import cc.spray.typeconversion.DefaultMarshallers
import cc.spray.Directives
import com.evecentral.dataaccess.StaticProvider
import com.evecentral.{Database, PoisonCache, OrderCacheActor, ECActorPool}
import org.slf4j.LoggerFactory

class OldUploadServiceActor extends Actor with Directives with DefaultMarshallers {

  private val log = LoggerFactory.getLogger(getClass)

  def mailActor = {
    val r = Actor.registry.actorsFor[MailDispatchActor]
    r(0)
  }


  def insertData(marketType: Int, regionId: Long, rows: Seq[UploadRecord]) {
    import net.noerd.prequel.SQLFormatterImplicits._

    Database.coreDb.transaction {
      tx =>
        tx.executeBatch("INSERT INTO archive_market (regionid, systemid, stationid, typeid,bid,price, orderid, minvolume, volremain, " +
          "volenter, issued, duration, range, reportedby, source)" +
          "VALUES( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST (? AS INTERVAL), ?, 0, 'evec_upload_cache')") {
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
          "VALUES( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST (? AS INTERVAL), ?, ?)") {
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

  def procData(data: UploadMessage) {
    val rows = data.orders
    if (data.valid) {
      val regionId = data.regionId
      val typeId = data.typeId
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
        procData(new CsvUploadMessage(rows))
      ctx.complete("Beginning your upload of " + typeid + "\nTypeID: 0 RegionID: 0\nComplete! Thank you for your contribution to EVE-Central.com!")
    }
  }
}
