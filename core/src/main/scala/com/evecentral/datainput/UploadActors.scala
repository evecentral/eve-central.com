package com.evecentral.datainput

import akka.actor.Actor
import akka.dispatch.{RequiresMessageQueue, BoundedMessageQueueSemantics}

import com.evecentral.dataaccess.StaticProvider
import com.evecentral.{Database, PoisonCache, OrderCacheActor}
import com.evecentral.util.ActorNames
import org.slf4j.LoggerFactory
import spray.routing.{Directives, RequestContext}

import org.joda.time.DateTime
import spray.httpx.marshalling.BasicMarshallers

case class UploadTriggerEvent(typeId: Int, regionId: Long)

class UnifiedUploadParsingActor extends Actor with Directives with BasicMarshallers with RequiresMessageQueue[BoundedMessageQueueSemantics] {

  def storageActor = context.actorFor("/user/" + ActorNames.uploadstorage)

  private val log = LoggerFactory.getLogger(getClass)

  def receive = {
    case msg: String =>
      try {
        UnifiedParser(msg) match {
          case Some(unimsg) =>
            storageActor ! unimsg
          case None =>
            log.error("Unable to parse unified message due to wrong type")
        }
      } catch {
        case e : Exception => log.error("Parse error " + msg, e)
      }
    case _ =>
      log.error("Unknown unified message input")
  }

}

class UploadStorageActor extends Actor with RequiresMessageQueue[BoundedMessageQueueSemantics] {

  private val log = LoggerFactory.getLogger(getClass)

  def statCaptureActor = context.actorFor("/user/" + ActorNames.statCapture)

  override def preStart() {
  }

  def insertData(marketType: Int, regionId: Long, rows: Seq[UploadRecord]) {
    import net.noerd.prequel.SQLFormatterImplicits._
    Database.coreDb.transaction {
      tx =>
        tx.executeBatch("INSERT INTO archive_market (regionid, systemid, stationid, typeid,bid,price, orderid, minvolume, volremain, " +
          "volenter, issued, duration, range, reportedby, source)" +
          "VALUES( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST (? AS INTERVAL), ?, 0, 'evec_upload_cache')") {
          statement =>
            rows.foreach {
              row =>
                statement.executeWith(row.regionId, row.solarSystemId, row.stationId, row.marketTypeId, if (row.bid) 1 else 0, row.price,
                  row.orderId, row.minVolume, row.volRemain, row.volEntered,
                  row.issued, row.duration.toString + " days", row.range)
            }
        }

        tx.execute("DELETE FROM current_market WHERE regionid = ? AND typeid = ?", regionId, marketType.toLong)

        tx.executeBatch("INSERT INTO current_market (regionid, systemid, stationid, typeid," +
          "bid,price, orderid, minvolume, volremain, volenter, issued, duration, range, reportedby, reportedtime)" +
          "VALUES( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST (? AS INTERVAL), ?, ?, ?)") {
          statement =>
            rows.foreach {
              row =>
                statement.executeWith(row.regionId, row.solarSystemId, row.stationId, row.marketTypeId, if (row.bid) 1 else 0,
                  row.price, row.orderId, row.minVolume, row.volRemain,
                  row.volEntered, row.issued, row.duration.toString + " days", row.range, 0, row.generatedAt)
            }
        }
    }
  }

  def poisonCache(marketType: Int, regionId: Long) {
    val a = context.actorFor("/user/" + ActorNames.statCache)
    (a ! PoisonCache(StaticProvider.regionsMap(regionId), StaticProvider.typesMap(marketType)))
  }

  def filterBogons(rows: Seq[UploadRecord]): Boolean = {
    rows.foldLeft(true) {
      case (rest, row) =>
        rest && row.price > 0.0 && row.price < 1000000000000000.0 && row.volEntered > 0 && row.minVolume > 0 && row.duration > 0 && row.volRemain > 0
    }
  }


  def procData(data: UploadMessage) {
    val rows = data.orders
    if (data.valid) {
      val regionId = data.regionId
      val typeId = data.typeId
      val generatedAt = data.generatedAt
      filterBogons(rows) match {
        case true =>
          insertData(typeId, regionId, rows)
          poisonCache(typeId, regionId)
          statCaptureActor ! UploadTriggerEvent(typeId, regionId)
        case false =>
          log.debug("GeneratedAt time was out of bounds to be considered fresh")
      }

    }
  }

  def receive = {
    case rows: CsvUploadMessage =>
      procData(rows)
    case msg: UnifiedUploadMessage =>
      msg.rowsets.foreach(s => procData(s))
    case hist: UnifiedHistoryMessage =>
  }
}
