package com.evecentral.datainput

import org.joda.time.DateTime
import com.evecentral.frontend.DateFormats
import org.joda.time.format.ISODateTimeFormat

case class UploadCsvRow(line: String) {
  private[this] val fields = line.split(",")
  val price = fields(0).toDouble
  val volRemain = fields(1).toLong
  val marketTypeId = fields(2).toInt
  val range = fields(3).toInt
  val orderId = fields(4).toLong
  val volEntered = fields(5).toLong
  val minVolume = fields(6).toLong
  val bid = fields(7).toBoolean
  val issued = new DateTime(fields(8))
  val duration = fields(9).toInt
  val stationId = fields(10).toLong
  val regionId = fields(11).toLong
  val solarSystemId = fields(12).toLong
  val jumps = fields(13).toInt
  val generatedAt = { val dt = new DateTime(); val fmt = ISODateTimeFormat.dateTime(); fmt.print(dt); }

  override def toString = Seq("%0.2f".format(price), volRemain.toString, marketTypeId.toString, range.toString, orderId.toString,
    volEntered.toString, minVolume.toString, if (bid) "1" else "0", DateFormats.dateTime.print(issued), duration.toString, stationId.toString,
    regionId.toString, solarSystemId.toString,
    jumps.toString, generatedAt).mkString(",")
}
