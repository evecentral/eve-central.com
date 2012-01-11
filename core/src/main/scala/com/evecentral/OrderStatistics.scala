package com.evecentral

import dataaccess.MarketOrder
import scala.math._

case class StaticsResult(mean: Double, median: Double, fivePercent: Double, stddev: Double,  min: Double,  max: Double,  volume: Double) {

}

object OrderStatistics {
  
  import MarketOrder._
  
  def statistics(over: Seq[MarketOrder]) : Unit = {
    
  }

  def wavg(over: Seq[MarketOrder]): Double = over.isEmpty match {
    case false => over.foldLeft(0.0)((a,b) => b.weightPrice + a ) /
    over.foldLeft(0.0)((a,b) => b.volremain + a)
    case true => 0.0
  }

  def avg(over: Seq[MarketOrder]): Double = over.isEmpty match {
    case false => over.foldLeft(0.0)((a,b) => b + a ) / over.length
    case true => 0.0
  }
  
  def squaredDifference(value1: Double, value2: Double) : Double =  scala.math.pow(value1 - value2, 2.0)

  def stdDev(list: Seq[MarketOrder], average: Double) = list.isEmpty match {
    case false =>
      val squared = list.foldLeft(0.0)((x,y) => x + squaredDifference(y, average))
      sqrt(squared / list.length.toDouble)
    case true => 0.0
  }
}