package com.evecentral.datainput

import com.evecentral.mail.MailDispatchActor
import akka.actor.Actor
import Actor.actorOf
import cc.spray.typeconversion.DefaultMarshallers
import com.evecentral.dataaccess.StaticProvider
import com.evecentral.{Database, PoisonCache, OrderCacheActor, ECActorPool}
import org.slf4j.LoggerFactory
import cc.spray.{RequestContext, Directives}

import org.joda.time.DateTime
import akka.config.Supervision.OneForOneStrategy

case class UploadTriggerEvent(typeId: Int, regionId: Long)

case class OldUploadPayload(ctx: RequestContext, typename: Option[String], userid: Option[String],
														data: String, typeid: Option[String], region: Option[String])


class OldUploadParsingActor extends Actor with Directives with DefaultMarshallers {
	private val log = LoggerFactory.getLogger(getClass)

	def storageActor = { val r = Actor.registry.actorsFor[UploadStorageActor]; r(0) }

	def receive = {
		case OldUploadPayload(ctx, typename, userid, data, typeid, region) => {
			log.info("Processing upload payload for " + typeid)
			val lines = data.split("\n").tail
			val rows = lines.map(UploadCsvRow(_))
			if (rows.nonEmpty)
				storageActor ! (new CsvUploadMessage(rows))
			else
				log.info("Skipping blank upload from old sources")
			ctx.complete("Beginning your upload of " + typeid + "\nTypeID: 0 RegionID: 0\nComplete! Thank you for your contribution to EVE-Central.com!")
		}
	}
}

class UnifiedUploadParsingActor extends Actor with Directives with DefaultMarshallers {

	def storageActor = { val r = Actor.registry.actorsFor[UploadStorageActor]; r(0) }

	private val log = LoggerFactory.getLogger(getClass)

	def receive = {
		case msg : String =>
			log.info("Trying to parse unified message: " + msg)
			UnifiedParser(msg) match {
				case Some(unimsg) =>
					log.info("Parsed uni msg: " + unimsg)
					storageActor ! unimsg
				case None =>
					log.error("Unable to parse unified message due to wrong type")
			}
		case _ =>
			log.error("Unknown unified message input")
	}

}

class UploadStorageActor extends Actor {

	private val log = LoggerFactory.getLogger(getClass)

	def mailActor = {
		val r = Actor.registry.actorsFor[MailDispatchActor]
		r(0)
	}

	def statCaptureActor = { val r = Actor.registry.actorsFor[StatisticsCaptureActor]; r(0) }

	self.faultHandler = OneForOneStrategy(List(classOf[Throwable]), 100, 1000)

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
		val a = Actor.registry.actorsFor[OrderCacheActor]
		(a(0) ! PoisonCache(StaticProvider.regionsMap(regionId), StaticProvider.typesMap(marketType)))
	}

	def confirmGeneratedAt(generatedAt: DateTime, typeId: Int, regionId: Long) : Boolean = {
	if (generatedAt.isAfterNow || generatedAt.plusHours(1).isBeforeNow)
		false
	else {
		try {
			val dbtime = Database.coreDb.transaction {
				tx =>
					import net.noerd.prequel.SQLFormatterImplicits._
					tx.selectDateTime("SELECT reportedtime FROM current_market WHERE typeid = ? AND regionid = ? LIMIT 1", typeId, regionId)
			}
			dbtime.isBefore(generatedAt)
		} catch {
			case _ => { log.debug("GeneratedAt success since there isn't anything in the database"); true }
		}
	}

	}

	def procData(data: UploadMessage) {
		val rows = data.orders
		if (data.valid) {
			val regionId = data.regionId
			val typeId = data.typeId
			val generatedAt = data.generatedAt
			mailActor ! rows
			confirmGeneratedAt(generatedAt, typeId, regionId) match {
				case true =>
					insertData(typeId, regionId, rows)
					poisonCache(typeId, regionId)
					statCaptureActor ! UploadTriggerEvent(typeId, regionId)
					log.debug("Processing upload complete")
				case false =>
					log.debug("GeneratedAt time was out of bounds to be considered fresh")
			}

		}
	}

	def receive = {
		case rows : CsvUploadMessage =>
			log.info("Storing classic message")
			procData(rows)
		case msg : UnifiedUploadMessage =>
			log.info("Storing unified message")
			msg.rowsets.foreach(s => procData(s))
	}
}
