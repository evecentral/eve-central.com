package com.evecentral

import com.twitter.querulous.evaluator.QueryEvaluator

object Database {
  def coreDb = QueryEvaluator("localhost", "evec", "evec", "evec", Map[String,String](), "jdbc:postgresql")
}
