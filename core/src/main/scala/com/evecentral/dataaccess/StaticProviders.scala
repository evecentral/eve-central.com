package com.evecentral.dataaccess

import com.evecentral.Database

object QueryDefaults {
  val minQLarge: Long = 10001
  lazy val minQExceptions = List[Long](34, 35, 36, 37, 38, 39, 40, 11399).foldLeft(Map[Long, Long]()) {
    (i, s) => i ++ Map(s -> minQLarge)
  }

  def minQ(typeid: Long): Long = {
    minQExceptions.getOrElse(typeid, 1)
  }
}

/**
 * A provider of static data which has all be loaded into memory.
 */
object StaticProvider {
  lazy val systemsMap = {
    var m = Map[Long, String]()
    Database.coreDb.select("SELECT systemid,systemname FROM systems") {
      row =>
        val sysid = row.getLong("systemid")
        val name = row.getString("systemname")
        m = m ++ Map(sysid -> name)
    }
    m
  }

  lazy val stationsMap = {
    var m = Map[Long, String]()
    Database.coreDb.select("SELECT stationid,stationname FROM stations") {
      row =>
        val sysid = row.getLong("stationid")
        val name = row.getString("stationname")
        m = m ++ Map(sysid -> name)
    }
    m
  }

  lazy val regionsMap = {
    var m = Map[Long, String]()
    Database.coreDb.select("SELECT regionid,regionname FROM regions") {
      row =>
        val sysid = row.getLong("regionid")
        val name = row.getString("regionname")
        m = m ++ Map(sysid -> name)
    }
    m
  }

  lazy val typesMap = {
    var m = Map[Long, String]()
    Database.coreDb.select("SELECT typeid,typename FROM types") {
      row =>
        val sysid = row.getLong("typeid")
        val name = row.getString("typename")
        m = m ++ Map(sysid -> name)
    }
    m
  }

}
