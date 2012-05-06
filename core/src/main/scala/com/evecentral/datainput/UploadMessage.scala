package com.evecentral.datainput

import net.liftweb.json._
import org.joda.time.DateTime
import org.joda.time.format.{ISODateTimeFormat, DateTimeFormat}


trait UploadMessage {
  def orders : Seq[UploadRecord]
  def typeId : Int
  def regionId : Long
  def valid : Boolean
}

class CsvUploadMessage(rows: Seq[UploadCsvRow]) extends UploadMessage {
  def orders = rows

  private[this] val regionType = rows.map(row => (row.marketTypeId, row.regionId)).distinct
  val valid = regionType.length == 1
  val regionId = valid match { case true => regionType(0)._2 case _ => -1}
  val typeId = valid match { case true => regionType(0)._1 case _ => -1 }

}

case class InvalidRowsetException() extends Exception

class UnifiedRowset(record : Map[String, _], columns: Seq[String]) {
  override def toString = record.toString

  private[this] val rowmaps = record("rows").asInstanceOf[List[List[Any]]].map(r => columns.zip(r).toMap)

  val regionId = record("regionID") match {
    case n: BigInt => n.toLong
    case null => throw InvalidRowsetException()
  }

  val typeId = record("typeID") match {
    case n: BigInt => n.toLong
    case null => throw InvalidRowsetException()
  }

  val generatedAt : DateTime = { ISODateTimeFormat.dateTimeParser().parseDateTime(record("generatedAt").asInstanceOf[String]) }

}

class UnifiedUploadMessage(data: String) {
  import net.liftweb.json.JsonDSL._
  private[this] val json = parse(data)

  val resultType = (json \ "resultType" \\ classOf[JString])(0)
  val columns = (json \ "columns" \\ classOf[JString])
  val rowsets = (json \ "rowsets" \\ classOf[JArray])(0).map(rs => rs match { case m : Map[String ,_] => new UnifiedRowset(m, columns) } )

  println(rowsets)
}

