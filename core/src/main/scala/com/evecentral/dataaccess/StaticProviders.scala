package com.evecentral.dataaccess

import com.evecentral.Database

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

case class Region(regionid: Long,  name: String)
case class Station(stationid: Long, name: String, system: SolarSystem)
case class SolarSystem(systemid: Long, name: String, security: Double, region: Region, constellationid: Long)

/**
 * A provider of static data which has all be loaded into memory.
 */
object StaticProvider {

  /**
   * Maps a systemId to a solarsystem.
   */
  lazy val systemsMap = {
    var m = Map[Long, SolarSystem]()
    Database.coreDb.transaction {
      tx =>
      tx.selectAndProcess("SELECT systemid,systemname,security,regionid,constellationid FROM systems") {
        row =>
          val sysid = row.nextLong match { case Some(x) => x }
          val name = row.nextString match { case Some(x) => x }
          val security = row.nextDouble match { case Some(x) => x }
          val regionid = row.nextLong match { case Some(x) => x }
          val constellationid = row.nextLong match { case Some(x) => x }
          m = m ++ Map(sysid -> SolarSystem(sysid, name, security, regionsMap(regionid), constellationid))
      }
    }
    m
  }

  /**
   * Maps a station ID to a station
   */
  lazy val stationsMap = {
    var m = Map[Long, Station]()
    Database.coreDb.transaction{
      tx =>
        tx.selectAndProcess("SELECT stationid,stationname,systemid FROM stations") {
        row =>
          val staid = row.nextLong match { case Some(x) => x }
          val name = row.nextString match { case Some(x) => x }
          val sysid = row.nextLong match { case Some(x) => x }

          m = m ++ Map(staid -> Station(staid, name, systemsMap(sysid)))
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
            val sysid = row.nextLong match { case Some(x) => x }
            val name = row.nextString match { case Some(x) => x }
            m = m ++ Map(sysid -> Region(sysid, name))
    }
    }
    m
  }

  lazy val typesMap = {
    var m = Map[Long, String]()
    Database.coreDb.transaction {
      tx =>
        tx.selectAndProcess("SELECT typeid,typename FROM types") {
        row =>
          val sysid = row.nextLong match { case Some(x) => x }
          val name = row.nextString match { case Some(x) => x }
          m = m ++ Map(sysid -> name)
      }
    }
    m
  }

}
