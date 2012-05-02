package com.evecentral.datainput

import org.joda.time.DateTime
import com.evecentral.frontend.DateFormats
import org.joda.time.format.{DateTimeFormat, ISODateTimeFormat}

case class UploadCsvRow(line: String) {
  private[this] val fields = line.split(",")
  val price = fields(0).toDouble
  val volRemain = fields(1).toDouble.toLong
  val marketTypeId = fields(2).toInt
  val range = fields(3).toInt
  val orderId = fields(4).toLong
  val volEntered = fields(5).toDouble.toLong
  val minVolume = fields(6).toDouble.toLong
  val bid = fields(7).toBoolean
  val issued = {
    try {
      DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").parseDateTime(fields(8))
    } catch {
      case _ => DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS").parseDateTime(fields(8))
    }
  }
  val duration = fields(9).toDouble.toLong
  val stationId = fields(10).toLong
  val regionId = fields(11).toLong
  val solarSystemId = fields(12).toLong
  val jumps = fields(13).toInt
  val source = try {
    fields(14)
  } catch {
    case _ => "Unknown"
  }
  val generatedAt = { val dt = new DateTime(); val fmt = ISODateTimeFormat.dateTime(); fmt.print(dt); }

    override def toString = Seq("%.2f".format(price), volRemain.toString, marketTypeId.toString, range.toString, orderId.toString,
    volEntered.toString, minVolume.toString, if (bid) "1" else "0", DateFormats.sqlDateTime.print(issued), duration.toString, stationId.toString,
    regionId.toString, solarSystemId.toString,
    jumps.toString, source.toString, generatedAt).mkString(",")
}
