package com.evecentral

import com.twitter.querulous.evaluator.QueryEvaluator

object Database {
  def coreDb = QueryEvaluator("localhost", "evec", "evec", "evec", Map[String, String](), "jdbc:postgresql")

  def concatQuery(fieldName: String, items: Seq[Any]): String = {
    if (items.length == 0) "1=1"
    else
      items.map({
        fieldName + " = " + _.toString
      }).mkString(" OR ")
  }
}
