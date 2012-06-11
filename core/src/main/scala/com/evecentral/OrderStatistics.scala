package com.evecentral

import dataaccess.{Region, MarketType, GetOrdersFor, MarketOrder}
import scala.math._
import akka.actor.{Scheduler, Actor}
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory

trait OrderStatistics {
  def volume : Long
  def wavg : Double
  def avg : Double
  def variance : Double
  def stdDev : Double

  def median : Double
  def fivePercent : Double

  def max : Double
  def min : Double
	def highToLow : Boolean
}

class CachedOrderStatistics(val forQuery: GetOrdersFor, private[this] var from: OrderStatistics) extends OrderStatistics {
  private val _volume = from.volume
  private val _wavg = from.wavg
  private val _avg = from.avg
  private val _variance = from.variance
  private val _stdDev = from.stdDev
  private val _median = from.median
  private val _fivePercent = from.fivePercent
  private val _max = from.max
  private val _min = from.min
	private val _highToLow = from.highToLow

  override def volume = _volume
  override def wavg = _wavg
  override def avg = _avg
  override def variance = _variance
  override def stdDev = _stdDev
  override def median = _median
  override def fivePercent = _fivePercent
  override def max = _max
  override def min = _min
	override def highToLow = _highToLow

}

private class LazyOrderStatistics(over: Seq[MarketOrder], val highToLow: Boolean = false) extends OrderStatistics {
  override lazy val volume = OrderStatistics.volume(over)
  override lazy val wavg = OrderStatistics.wavg(over, volume)
  override lazy val avg = OrderStatistics.avg(over)
  override lazy val variance = OrderStatistics.variance(over, avg)
  override lazy val stdDev = OrderStatistics.stdDev(variance)
  lazy val sorted = OrderStatistics.sorted(over, highToLow)

  override lazy val median = OrderStatistics.median(sorted, (volume.toDouble / 2.0))
  override lazy val fivePercent = OrderStatistics.buyup(sorted, (volume * .05).toLong)

  override lazy val max = OrderStatistics.max(over)
  override lazy val min = OrderStatistics.min(over)
}

object OrderStatistics {

  import MarketOrder._


  def apply(over: Seq[MarketOrder], highToLow: Boolean = false) : OrderStatistics = {
	  if (over.length < 10)
		  new LazyOrderStatistics(over, highToLow)
	  else {
		  val l = new LazyOrderStatistics(over, highToLow)
		  val neworders = over.filter(order => if (highToLow) true else order.price < (l.wavg*3))
		  new LazyOrderStatistics(neworders, highToLow)
	  }


  }

  def cached(query: GetOrdersFor, data: OrderStatistics) : CachedOrderStatistics = {
    new CachedOrderStatistics(query, data)
  }
  
  def max(over: Seq[MarketOrder]) : Double = {
    over.isEmpty match {
      case false =>
        over.maxBy(_.price).price
      case true =>
        0.0
    }
  }
  
  def min(over: Seq[MarketOrder]) : Double = {
    over.isEmpty match {
      case false =>
        over.minBy(_.price).price
      case true =>
        0.0
    }
  }
  
  def volume(over: Seq[MarketOrder]): Long = {
    over.isEmpty match {
      case false =>
        over.foldLeft(0.toLong)((a, b) => a + b.volenter)
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

case class RegisterCacheFor(cache: CachedOrderStatistics)

case class GetCacheFor(query: GetOrdersFor, highToLow: Boolean)

case class PoisonCache(region: Region, marketType: MarketType)

case class PoisonAllCache()

class OrderCacheActor extends Actor {
  
  private val cacheHash = scala.collection.mutable.HashMap[GetCacheFor, OrderStatistics]()
  private val log = LoggerFactory.getLogger(getClass)

  override def preStart() {
    cacheHash.clear()
    Scheduler.schedule(self, PoisonAllCache, 5, 60, TimeUnit.MINUTES)

  } 
  
  
  def receive = {
    case gcf : GetCacheFor =>
      self.channel ! cacheHash.get(gcf)
    case RegisterCacheFor(cached) =>
      cacheHash.put(GetCacheFor(cached.forQuery, cached.highToLow), cached)
    case PoisonAllCache =>
      log.info("Poisoning all cache entries")
      cacheHash.clear()
    case PoisonCache(region, mtype) => // Slow poisoning of the cache for regions and types
      // @TODO: Make this non-linear-time
      cacheHash.keySet.foreach(of => if ((of.query.regions.contains(region.regionid) || of.query.regions.isEmpty) &&
        of.query.types.contains(mtype.typeid)) { cacheHash.remove(of); log.info("Removing from cache " + of) } )
  }
}
