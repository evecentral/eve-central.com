package com.evecentral

import net.noerd.prequel.{PoolConfig, DatabaseConfig}

import org.postgresql.Driver

object Database {

	def coreDb = dbconfig

	private[this] val poolconfig = PoolConfig(maxActive = 30,  maxIdle = 5, minIdle = 5)

	private[this] val dbconfig = DatabaseConfig(
    driver = "org.postgresql.Driver",
    jdbcURL = "jdbc:postgresql://localhost/evec",
    username = "evec",
    password =  "evec",
		poolConfig = poolconfig
	)

  def concatQuery(fieldName: String, items: Seq[Any]): String = {
    if (items.length == 0) "1=1"
    else
      items.map({
        fieldName + " = " + _.toString
      }).mkString(" OR ")
  }
}
