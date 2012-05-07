package com.evecentral.datainput

import com.evecentral.mail.MailDispatchActor
import akka.actor.Actor
import Actor._
import cc.spray.typeconversion.DefaultMarshallers
import com.evecentral.dataaccess.StaticProvider
import com.evecentral.{Database, PoisonCache, OrderCacheActor, ECActorPool}
import org.slf4j.LoggerFactory
import cc.spray.{RequestContext, Directives}


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
			val unimsg = new UnifiedUploadMessage(msg)
			log.info("Parsed uni msg: " + unimsg)
			storageActor ! unimsg
		case _ =>
			log.error("Unknown unified message input")
	}

}

class UploadStorageActor extends Actor with Directives with DefaultMarshallers {

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
						rows.foreach {
							row =>
								statement.executeWith(row.regionId, row.solarSystemId, row.stationId, row.marketTypeId, if (row.bid) 1 else 0, row.price,
									row.orderId, row.minVolume, row.volRemain, row.volEntered,
									row.issued, row.duration.toString + " days", row.range)
						}
				}

				tx.execute("DELETE FROM current_market WHERE regionid = ? AND typeid = ?", regionId, marketType.toLong)

				tx.executeBatch("INSERT INTO current_market (regionid, systemid, stationid, typeid," +
					"bid,price, orderid, minvolume, volremain, volenter, issued, duration, range, reportedby)" +
					"VALUES( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST (? AS INTERVAL), ?, ?)") {
					statement =>
						rows.foreach {
							row =>
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
		case rows : CsvUploadMessage =>
			log.info("Storing classic message")
			procData(rows)
		case msg : UnifiedUploadMessage =>
			log.info("Storing unified message")
			msg.rowsets.foreach(s => procData(s))
	}
}
