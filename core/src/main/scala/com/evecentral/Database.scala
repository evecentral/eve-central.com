package com.evecentral

import com.twitter.querulous.evaluator.QueryEvaluator

object Database {
  def coredb = QueryEvaluator("localhost", "evec", "evec", "evec", Map[String,String](), "jdbc:postgresql")
}
