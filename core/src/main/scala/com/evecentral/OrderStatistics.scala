package com.evecentral

import dataaccess.MarketOrder
import scala.math._

class OrderStatistics(over: Seq[MarketOrder], highToLow: Boolean = false) {
  lazy val volume = OrderStatistics.volume(over)
  lazy val wavg = OrderStatistics.wavg(over, volume)
  lazy val avg = OrderStatistics.avg(over)
  lazy val variance = OrderStatistics.variance(over, avg)
  lazy val stdDev = OrderStatistics.stdDev(variance)
  lazy val sorted = OrderStatistics.sorted(over, highToLow)
  
  lazy val median = OrderStatistics.median(sorted, (volume.toDouble / 2.0))
  lazy val fivePercent = OrderStatistics.buyup(sorted, (volume * .05).toLong)
}

object OrderStatistics {

  import MarketOrder._


  def apply(over: Seq[MarketOrder], highToLow: Boolean = false) : OrderStatistics = {
    new OrderStatistics(over, highToLow)
  }
  
  def volume(over: Seq[MarketOrder]): Long = {
    over.isEmpty match {
      case false =>
        over.foldLeft(0)((a, b) => a + b.volenter)
      case true =>
        0
    }
  }

  def sorted(over: Seq[MarketOrder], reverse: Boolean) : Seq[MarketOrder] = {
    reverse match {
      case true => over.sortBy(n => -n.price)
      case false => over.sortBy(n => n.price)
    }
  }
  
  /**
   * A bad imperative median
   *
   */
  def median(sorted: Seq[MarketOrder], volumeTo: Double): Double = {
    sorted.isEmpty match {

      case false =>
        var rest = sorted
        var sumVolume: Long = 0
        
        while (sumVolume <= volumeTo) {
          sumVolume += rest.head.volenter
          if (sumVolume < volumeTo)
            rest = rest.tail
        }
        if (sorted.length % 2 == 0 && rest.length > 1)
          (rest.head.price + rest.tail.head.price) / 2.0
        else
          rest.head.price
      case true =>
        0.0
    }
  }

  def buyup(sorted: Seq[MarketOrder], volumeTo: Long): Double = {
    sorted.isEmpty match {

      case false =>
        var left = sorted
        var sumVolume: Long = 0
        var orders : List[MarketOrder] = List[MarketOrder]()
        
        while (sumVolume <= volumeTo) {
          sumVolume += left.head.volenter
          orders =  List[MarketOrder](left.head) ++ orders
          left = left.tail
        }
        wavg(orders, sumVolume)
      case true => 0.0
        
    }
  }

  def wavg(over: Seq[MarketOrder], volume: Long): Double = over.isEmpty match {
    case false => over.foldLeft(0.0)((a, b) => b.weightPrice + a) /
      volume
    case true => 0.0
  }

  def avg(over: Seq[MarketOrder]): Double = over.isEmpty match {
    case false => over.foldLeft(0.0)((a, b) => b + a) / over.length.toDouble
    case true => 0.0
  }

  def stdDev(variance: Double): Double = variance match {
    case 0.0 => 0.0
    case y => sqrt(y)
  }

  def squaredDifference(value1: Double, value2: Double): Double = scala.math.pow(value1 - value2, 2.0)

  def variance(list: Seq[MarketOrder], average: Double) = list.isEmpty match {
    case false =>
      val squared = list.foldLeft(0.0)((x, y) => x + squaredDifference(y, average))
      squared / list.length.toDouble
    case true => 0.0
  }
}