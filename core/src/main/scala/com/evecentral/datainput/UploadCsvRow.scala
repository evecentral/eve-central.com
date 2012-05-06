package com.evecentral.datainput

import org.joda.time.DateTime
import com.evecentral.frontend.DateFormats
import org.joda.time.format.{DateTimeFormat, ISODateTimeFormat}

trait UploadRecord {
  def price : Double
  def volRemain : Long
  def marketTypeId : Int
  def range : Int
  def orderId : Long
  def volEntered : Long
  def minVolume : Long
  def bid : Boolean
  def issued : DateTime
  def duration : Long
  def stationId : Long
  def regionId : Long
  def solarSystemId : Long
  def jumps : Int
  def source : String
  def generatedAt : DateTime
  lazy val generatedAtString = { val fmt = ISODateTimeFormat.dateTime(); fmt.print(generatedAt); }
  override def toString = Seq("%.2f".format(price), volRemain.toString, marketTypeId.toString, range.toString, orderId.toString,
    volEntered.toString, minVolume.toString, if (bid) "1" else "0", DateFormats.sqlDateTime.print(issued), duration.toString, stationId.toString,
    regionId.toString, solarSystemId.toString,
    jumps.toString, source.toString, generatedAtString).mkString(",")
}

case class UnifiedRow(price: Double, volRemain: Long, marketTypeId: Int,
                       range: Int, orderId: Long,
                       volEntered: Long,
                       minVolume: Long,
                       bid: Boolean,
                       issued: DateTime,
                       duration: Long,
                       stationId: Long,
                       regionId : Long,
                       solarSystemId: Long,
                       jumps: Int,
                       source: String,
                       generatedAt: DateTime) extends UploadRecord

case class UploadCsvRow(line: String) extends UploadRecord {
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
  val generatedAt = new DateTime();


}
