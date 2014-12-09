package com.evecentral.datainput

import com.evecentral.dataaccess.StaticProvider
import net.liftweb.json._
import org.joda.time.DateTime
import org.joda.time.format.{ISODateTimeFormat}


/**
 * Represents a single order upload message
 */
trait UploadMessage {
  def orders: Seq[UploadRecord]
  def typeId: Int
  def regionId: Long
  def valid: Boolean
  def generatedAt: DateTime
}

/**
 * A base type for any unified format container message
 */
trait UnifiedMessage {
  def originalMessage: JValue
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

trait UnifiedBaseRowset {

  protected val record: Map[String, _]

  protected val columns: Seq[String]

  val generatedAt: DateTime = {
    ISODateTimeFormat.dateTimeParser().parseDateTime(record("generatedAt").asInstanceOf[String])
  }

  val regionId = record("regionID") match {
    case n: BigInt => n.toLong
    case s: String => s.toLong
    case null => throw InvalidRowsetException()
  }

  val typeId = record("typeID") match {
    case n: BigInt => n.toInt
    case s: String => s.toInt
    case null => throw InvalidRowsetException()
  }

  protected[this] val rowmaps = record("rows").asInstanceOf[List[List[Any]]].map(r => columns.zip(r).toMap)

}

class HistoryRowset(protected val record: Map[String, _], protected val columns: Seq[String]) extends UnifiedBaseRowset {

  val history = rowmaps.map {
    row =>
      val date = ISODateTimeFormat.dateTimeParser().parseDateTime(row("date").asInstanceOf[String])
      val qty = row("quantity") match {
        case n: BigInt => n.toLong
      }
      val low = row("low") match {
        case bi: BigInt => bi.toDouble
        case n: Double => n
        case f: Float => f.toDouble
        case dec: BigDecimal => dec.toDouble
      }
      val high = row("high") match {
        case bi: BigInt => bi.toDouble
        case n: Double => n
        case f: Float => f.toDouble
        case dec: BigDecimal => dec.toDouble
      }
      val avg = row("average") match {
        case bi: BigInt => bi.toDouble
        case n: Double => n
        case f: Float => f.toDouble
        case dec: BigDecimal => dec.toDouble
      }
      HistoryRow(regionId, typeId, date, qty, low, high, avg)
  }

}

class UnifiedRowset(protected val record: Map[String, _], protected val columns: Seq[String], source: String) extends UploadMessage with UnifiedBaseRowset {
  //override def toString = record.toString


  val orders = rowmaps.map {
    row =>
      val price = row("price") match {
        case d: Double => d
        case f: Float => f.toDouble
        case dec: BigDecimal => dec.toDouble
        case bi: BigInt => bi.toDouble
      }
      val volremain = row("volRemaining") match {
        case n: BigInt => n.toLong
      }
      val volenter = row.get("volEntered") match {
        case Some(n: BigInt) => n.toLong
        case None => volremain
      }
      val range = row("range") match {
        case n: BigInt => n.toInt
        case i: Int => i
      }
      val orderID = row("orderID") match {
        case n: BigInt => n.toLong
      }
      val minvol = row("minVolume") match {
        case n: BigInt => n.toLong
      }
      val issue = ISODateTimeFormat.dateTimeParser().parseDateTime(row("issueDate").asInstanceOf[String])
      val duration = row("duration") match {
        case n: BigInt => n.toLong
      }
      val stationid = row("stationID") match {
        case n: BigInt => n.toLong
      }
      val solarsystemid = row.get("solarSystemID") match {
        case Some(n: BigInt) => n.toLong
        case None => StaticProvider.stationsMap.get(stationid).map { _.system.systemid }.getOrElse(-1.toLong)
      }
      val bid = row("bid").asInstanceOf[Boolean]
      UnifiedRow(price, volremain, typeId, range, orderID, volenter, minvol, bid, issue, duration,
        stationid, regionId, solarsystemid, 0, source, generatedAt)
  }

  val valid = true
}

object UnifiedParser {
  private[this] def buildOrderMessage(json: JValue): UnifiedMessage = {
    val columns = (json \ "columns" \\ classOf[JString])
    val gen_name = (json \ "generator" \ "name") \\ classOf[JString]
    val gen_ver = (json \ "generator" \ "version") \\ classOf[JString]

    val source = gen_name + " " + gen_ver

    val rowsets = (json \ "rowsets" \\ classOf[JArray])(0).map(rs => rs match {
      case m: Map[String, _] => new UnifiedRowset(m, columns, source)
    })
    UnifiedUploadMessage(json, columns, source, rowsets)
  }

  private def buildHistoryMessage(json: JValue): UnifiedMessage = {
    val columns = (json \ "columns" \\ classOf[JString])
    val gen_name = (json \ "generator" \ "name") \\ classOf[JString]
    val gen_ver = (json \ "generator" \ "version") \\ classOf[JString]
    val rowsets = (json \ "rowsets" \\ classOf[JArray])(0).map(rs => rs match {
      case m: Map[String, _] => new HistoryRowset(m, columns)
    })
    UnifiedHistoryMessage(json, rowsets)
  }

  def apply(data: String): Option[UnifiedMessage] = {
    import net.liftweb.json.JsonDSL._

    val json = parse(data)

    val resultType = (json \ "resultType" \\ classOf[JString])(0)
    resultType match {
      case "orders" =>
        Some(buildOrderMessage(json))
      case "history" =>
        Some(buildHistoryMessage(json))
      case _ => None
    }
  }
}

case class UnifiedUploadMessage(originalMessage: JValue,
                                columns: Seq[String], source: String,
                                rowsets: Seq[UnifiedRowset]) extends UnifiedMessage

case class UnifiedHistoryMessage(originalMessage: JValue, rowsets: Seq[HistoryRowset]) extends UnifiedMessage


