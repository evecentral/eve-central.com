package com.evecentral

import net.noerd.prequel.DatabaseConfig

import org.postgresql.Driver

object Database {

	private[this] var hasInited = false

	private[this] def dummyTx {
		dbconfig.transaction { tx =>
			tx.select("SELECT 1 = 1")
			{ row => row.nextBoolean }
		}
		hasInited = true
	}

	def coreDb : DatabaseConfig = {
		dbconfig.synchronized {
			if (!hasInited)
				dummyTx
			dbconfig
		}
	}

	private[this] val dbconfig = DatabaseConfig(
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
