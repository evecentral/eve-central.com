package com.evecentral.datainput

import net.liftweb.json._
import org.joda.time.DateTime
import org.joda.time.format.{ISODateTimeFormat, DateTimeFormat}


trait UploadMessage {
	def orders: Seq[UploadRecord]

	def typeId: Int

	def regionId: Long

	def valid: Boolean

	def generatedAt: DateTime
}

class CsvUploadMessage(rows: Seq[UploadCsvRow]) extends UploadMessage {
	def orders = rows

	private[this] val regionType = rows.map(row => (row.marketTypeId, row.regionId)).distinct
	val valid = regionType.length == 1
	val regionId = valid match {
		case true => regionType(0)._2
		case _ => -1
	}
	val typeId = valid match {
		case true => regionType(0)._1
		case _ => -1
	}

	val generatedAt = DateTime.now

}

case class InvalidRowsetException() extends Exception

class UnifiedRowset(record: Map[String, _], columns: Seq[String], source: String) extends UploadMessage {
	override def toString = record.toString

	val generatedAt: DateTime = {
		ISODateTimeFormat.dateTimeParser().parseDateTime(record("generatedAt").asInstanceOf[String])
	}

	private[this] val rowmaps = record("rows").asInstanceOf[List[List[Any]]].map(r => columns.zip(r).toMap)

	val regionId = record("regionID") match {
		case n: BigInt => n.toLong
		case null => throw InvalidRowsetException()
	}

	val typeId = record("typeID") match {
		case n: BigInt => n.toInt
		case null => throw InvalidRowsetException()
	}

val orders = rowmaps.map { row =>
	val price = row("price") match { case d : Double => d case f : Float => f.toDouble case dec : BigDecimal => dec.toDouble case bi : BigInt => bi.toDouble }
	val volremain = row("volRemaining") match { case n : BigInt => n.toLong }
	val volenter = row("volEntered") match { case n : BigInt => n.toLong }
	val range = row("range") match { case n : BigInt => n.toInt case i : Int => i }
	val orderID = row("orderID") match { case n : BigInt => n.toLong }
	val minvol = row("minVolume") match { case n : BigInt => n.toLong }
	val issue = ISODateTimeFormat.dateTimeParser().parseDateTime(row("issueDate").asInstanceOf[String])
	val duration = row("duration") match { case n : BigInt => n.toLong }
	val stationid = row("stationID") match { case n : BigInt => n.toLong }
	val solarsystemid = row("solarSystemID") match { case n : BigInt => n.toLong }
	val bid = row("bid").asInstanceOf[Boolean]
	UnifiedRow(price, volremain, typeId, range, orderID, volenter, minvol, bid, issue, duration,
		stationid, regionId, solarsystemid, 0, source, generatedAt)
}

	val valid = true
}

object UnifiedParser {
	private[this] def buildOrderMessage(json: JValue) : UnifiedUploadMessage = {
		val columns = (json \ "columns" \\ classOf[JString])
		val gen_name = (json \ "generator" \ "name") \\ classOf[JString]
		val gen_ver = (json \ "generator" \ "version") \\ classOf[JString]

		val source = gen_name + " " + gen_ver

		val rowsets = (json \ "rowsets" \\ classOf[JArray])(0).map(rs => rs match {
			case m: Map[String, _] => new UnifiedRowset(m, columns, source)
		})
		UnifiedUploadMessage(columns, source, rowsets)
	}

	def apply(data: String) : Option[UnifiedUploadMessage] = {
		import net.liftweb.json.JsonDSL._

		val json = parse(data)

		val resultType = (json \ "resultType" \\ classOf[JString])(0)
		resultType match {
			case "orders" =>
				Some(buildOrderMessage(json))
			case _ => None
		}
	}
}

case class UnifiedUploadMessage(columns: Seq[String], source: String, rowsets: Seq[UnifiedRowset])


