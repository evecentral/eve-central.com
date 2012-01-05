package com.evecentral

import net.noerd.prequel.DatabaseConfig

import org.postgresql.Driver

object Database {

  def coreDb = DatabaseConfig(
    driver = "org.postgresql.Driver",
    jdbcURL = "jdbc:postgresql://localhost/evec",
    username = "evec",
    password =  "evec"
  )

  def concatQuery(fieldName: String, items: Seq[Any]): String = {
    if (items.length == 0) "1=1"
    else
      items.map({
        fieldName + " = " + _.toString
      }).mkString(" OR ")
  }
}
