package com.evecentral.dataaccess

import com.evecentral.Database

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
