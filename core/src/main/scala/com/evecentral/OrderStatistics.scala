package com.evecentral

import akka.actor.Actor
import com.evecentral.dataaccess.{GetOrdersFor, MarketType, Region, _}
import org.apache.commons.collections.map.LRUMap
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.math._

trait OrderStatistics {
  def volume: Long
  def wavg: Double
  def avg: Double
  def variance: Double
  def stdDev: Double
  def median: Double
  def fivePercent: Double
  def max: Double
  def min: Double
  def highToLow: Boolean
  def generated: DateTime
}

case class CachedOrderStatistics(forQuery: GetOrdersFor, volume: Long, wavg: Double, avg: Double, variance: Double,
                                 stdDev: Double, median: Double, fivePercent: Double, max: Double, min: Double,
                                 highToLow: Boolean, generated: DateTime) extends OrderStatistics

private class LazyOrderStatistics(over: Seq[MarketOrder], val highToLow: Boolean = false) extends OrderStatistics {
  override lazy val volume = OrderStatistics.volume(over)
  override lazy val wavg = OrderStatistics.wavg(over, volume)
  override lazy val avg = OrderStatistics.avg(over)
  override lazy val variance = OrderStatistics.variance(over, avg)
  override lazy val stdDev = OrderStatistics.stdDev(variance)
  lazy val sorted = OrderStatistics.sorted(over, highToLow)

  override lazy val median = OrderStatistics.median(sorted, volume.toDouble / 2.0)
  override lazy val fivePercent = OrderStatistics.buyup(sorted, (volume * .05).toLong)

  override lazy val max = OrderStatistics.max(over)
  override lazy val min = OrderStatistics.min(over)
  // Instantiating a lazy will always return the time the stats were built
  override lazy val generated = DateTime.now()
}

object OrderStatistics {

  import com.evecentral.dataaccess.MarketOrder._

  def apply(over: Seq[MarketOrder], highToLow: Boolean = false): OrderStatistics = {
    val overFiltered = over.filter(_.price > 0.15) // Limit prices to avoid 0.01 ISKers

    if (over.length < 10) {
      new LazyOrderStatistics(overFiltered, highToLow)
    } else {
      // This double computes statistics for filtering by weighted average
      val allStats = new LazyOrderStatistics(overFiltered, highToLow)
      highToLow match {
        case true => new LazyOrderStatistics(overFiltered.filter(order => order.price > (allStats.wavg / 3)), highToLow)
        case false => new LazyOrderStatistics(overFiltered.filter(order => order.price < (allStats.wavg * 3)), highToLow)
      }
    }
  }

  def cached(query: GetOrdersFor, data: OrderStatistics): CachedOrderStatistics = {
    CachedOrderStatistics(query, data.volume, data.wavg, data.avg, data.variance, data.stdDev, data.median,
      data.fivePercent, data.max, data.min, data.highToLow, data.generated)
  }

  def max(over: Seq[MarketOrder]): Double = {
    over.isEmpty match {
      case false =>
        over.maxBy(_.price).price
      case true =>
        0.0
    }
  }

  def min(over: Seq[MarketOrder]): Double = {
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

  def sorted(over: Seq[MarketOrder], reverse: Boolean): Seq[MarketOrder] = {
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

        while (sumVolume <= volumeTo || rest.nonEmpty) {
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
        val orders = Seq.newBuilder[MarketOrder]

        while (sumVolume <= volumeTo || left.nonEmpty) {
          sumVolume += left.head.volenter
          orders += left.head
          left = left.tail
        }
        wavg(orders.result(), sumVolume)
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

case class GetCachedRegion(region: Region)

case class PoisonCache(region: Region, marketType: MarketType)

case class PoisonAllCache()

class OrderCacheActor extends Actor {
  val periodicExpire = false
  implicit val ec = context.dispatcher

  type LLGCF = scala.collection.mutable.HashSet[GetCacheFor]

  private val typeQueryCache = StaticProvider.typesMap.values.foldLeft(Map[Long, LLGCF]()) {
    (b, a) => b ++ Map(a.typeid -> new LLGCF())
  }

  private val cacheLruHash = new org.apache.commons.collections.map.LRUMap(100000)
  private val regionLru = StaticProvider.regionsMap.values.foldLeft(Map[Region, LRUMap]()) {
    (b, a) => b ++ Map[Region, LRUMap](a -> new LRUMap(10000))
  }
  private val log = LoggerFactory.getLogger(getClass)

  override def preStart() {
    cacheLruHash.clear()
    regionLru.foreach {
      l => l._2.clear()
    }
    if (periodicExpire)
      context.system.scheduler.schedule(1.minute, 15.minutes, self, PoisonAllCache())
  }


  def receive = {
    case gcf: GetCacheFor =>
      val res = cacheLruHash.get(gcf)
      res match {
        case os: OrderStatistics =>
          sender ! Some(os)
        case _ =>
          sender ! None
      }
    case GetCachedRegion(region) =>
      val mi = regionLru(region).mapIterator()
      val mm = scala.collection.mutable.Map[GetCacheFor, OrderStatistics]()
      while (mi.hasNext) {
        val k = mi.getKey.asInstanceOf[GetCacheFor]
        val v = mi.getValue.asInstanceOf[OrderStatistics]
        mm.put(k, v)
        mi.next()
      }
    case RegisterCacheFor(cached) =>
      val present = cached.forQuery.types.forall(n => typeQueryCache.contains(n))
      if (present) {
        val gcf = GetCacheFor(cached.forQuery, cached.highToLow)
        cacheLruHash.put(gcf, cached)
        cached.forQuery.types.foreach(typeQueryCache(_) += gcf)
        if (cached.forQuery.systems.isEmpty) // Only store when systems are empty
          cached.forQuery.regions.foreach {
            regionid =>
              regionLru(StaticProvider.regionsMap(regionid)).put(gcf, cached) // Register per regions
          }
      } else {
        log.warn("Generated statistics for something non-marketable. I'm confused: " + cached.forQuery.types.headOption)
      }

    case PoisonAllCache() =>
      log.info("Poisoning all cache entries")
      cacheLruHash.clear()
      typeQueryCache.foreach(_._2.clear())
      regionLru.foreach(_._2.clear)

    case PoisonCache(region, mtype) => // Poisoning of the cache for regions and types
      val ls = typeQueryCache(mtype.typeid)

      val lsf = ls.filter({
        case of: GetCacheFor =>
          (of.query.regions.contains(region.regionid) || of.query.regions.isEmpty)
        case _ =>
          false
      })

      lsf.foreach {
        case gcf: GetCacheFor =>
          ls.remove(gcf)
          cacheLruHash.remove(gcf)
          gcf.query.regions.foreach {
            regionid => regionLru(StaticProvider.regionsMap(regionid)).remove(gcf)
          } // Remove per region LRU cache
      }

  }
}
