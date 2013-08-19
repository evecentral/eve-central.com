package com.evecentral.dataaccess

import com.evecentral.Database
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper

object StationNameUtility {
  def shorten(name: String): String = {
    try {
      val split = "Moon ".r.replaceAllIn(name, "M").split(" - ")
      val head = split.reverse.tail.reverse.mkString(" - ") // I'm sorry
      val words = split.last.split(" ").fold("")((c, s) => c + s.charAt(0))
      head + " - " + words
    } catch {
      case e: StringIndexOutOfBoundsException => name
    }
  }
}


object QueryDefaults {
  val minQLarge: Long = 10001
  lazy val minQExceptions = List[Long](34, 35, 36, 37, 38, 39, 40, 11399).foldLeft(Map[Long, Long]()) {
    (i, s) => i ++ Map(s -> minQLarge)
  }

  /**
   * Determine a sane minimum quantity. For minerals, this is usually set higher than
   * other values. The minQExceptions list maps the quantities.
   */
  def minQ(typeid: Long): Long = {
    minQExceptions.getOrElse(typeid, 1)
  }
}

trait BaseRegion {
  val name: String
  val regionid: Long
}

case object AnyRegion extends BaseRegion {
  val regionid: Long = 0
  val name = "Any Region"
}
/** Meta regions */
case object AllEmpireRegions extends BaseRegion {
  val regionid: Long = -1L
  val name = "All Empire"
}

case class Region(regionid: Long, name: String) extends BaseRegion

case class Station(stationid: Long, name: String, shortName: String, system: SolarSystem)

case class SolarSystem(systemid: Long, name: String, security: Double, region: Region, constellationid: Long)

case class MarketType(typeid: Long, name: String, group: Long)

object JacksonMapper {
  def serialize[T](t: T): String = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.writeValueAsString(t)
  }
}

object LookupHelper {

  def lookupSystem(text: String): SolarSystem = {
    try {
      StaticProvider.systemsMap(text.toLong)
    } catch {
      case _ : Throwable => StaticProvider.systemsByName(text)
    }
  }

  def lookupRegion(text: String): Region = {
    try {
      StaticProvider.regionsMap(text.toLong)
    } catch {
      case _ : Throwable => StaticProvider.regionsByName(text)
    }
  }

  def lookupType(text: String): MarketType = {
    try {
      StaticProvider.typesMap(text.toLong)
    } catch {
      case _ : Throwable => StaticProvider.typesByName(text)
    }
  }
}

/**
 * A provider of static data which has all be loaded into memory.
 */
object StaticProvider {

  /**
   * A list of regions considered to be in Empire Space
   */
  lazy val empireRegions = List[Long](10000001, 10000002, 10000016, 10000020, 10000028, 10000030, 10000032, 10000033,
    10000043, 10000049, 10000037, 10000038, 10000036, 10000052, 10000064, 10000065, 10000067,
    10000068, 10000054, 10000042, 10000044, 10000048).map(id => regionsMap(id))


  /**
   * Maps a systemId to a solarsystem.
   */
  lazy val systemsMap = {
    var m = Map[Long, SolarSystem]()
    Database.coreDb.transaction {
      tx =>
        tx.selectAndProcess("SELECT systemid,systemname,security,regionid,constellationid FROM systems") {
          row =>
            val sysid = row.nextLong.get
            val name = row.nextString.get
            val security = row.nextDouble.get
            val regionid = row.nextLong.get
            val constellationid = row.nextLong.get

            m = m ++ Map(sysid -> SolarSystem(sysid, name, security, regionsMap(regionid), constellationid))
        }
    }
    m
  }

  lazy val systemsByName = {
    systemsMap.foldLeft(Map[String, SolarSystem]()) {
      (maap, solar) => maap ++ Map(solar._2.name -> solar._2)
    }
  }

  lazy val typesByName = {
    typesMap.foldLeft(Map[String, MarketType]()) {
      (maap, typ) => maap ++ Map(typ._2.name -> typ._2)
    }
  }

  lazy val regionsByName = {
    regionsMap.foldLeft(Map[String, Region]()) {
      (maap, reg) => maap ++ Map(reg._2.name -> reg._2)
    }
  }

  /**
   * Maps a station ID to a station
   */
  lazy val stationsMap = {
    var m = Map[Long, Station]()
    Database.coreDb.transaction {
      tx =>
        tx.selectAndProcess("SELECT stationid,stationname,systemid FROM stations") {
          row =>
            val staid = row.nextLong.get
            val name = row.nextString.get
            val sysid = row.nextLong.get
            val shortName = StationNameUtility.shorten(name)
            m = m ++ Map(staid -> Station(staid, name, shortName, systemsMap(sysid)))
        }
    }
    m
  }

  /**
   * Maps a region ID to a Region
   */
  lazy val regionsMap = {
    var m = Map[Long, Region]()
    Database.coreDb.transaction {
      tx =>
        tx.selectAndProcess("SELECT regionid,regionname FROM regions") {
          row =>
            val sysid = row.nextLong.get
            val name = row.nextString.get

            m = m ++ Map(sysid -> Region(sysid, name))
        }
    }
    m
  }

  lazy val typesMap = {
    var m = Map[Long, MarketType]()
    Database.coreDb.transaction {
      tx =>
        tx.selectAndProcess("SELECT typeid,typename,marketgroup FROM types") {
          row =>
            val sysid = row.nextLong.get
            val name = row.nextString.get
            val marketGroup = row.nextLong.getOrElse(0L)
            m = m ++ Map(sysid -> MarketType(sysid, name, marketGroup))
        }
    }
    m
  }

}
