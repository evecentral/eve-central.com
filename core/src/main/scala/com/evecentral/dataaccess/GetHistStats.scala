package com.evecentral.dataaccess

import akka.actor.Actor
import com.evecentral.{Database, OrderStatistics}
import org.joda.time.DateTime

object GetHistStats {
	case class Request(marketType: MarketType, bid: Boolean, region: Either[AnyRegion, Region], system: Option[SolarSystem], from: DateTime, to: DateTime)
	case class CapturedOrderStatistics(median: Double, variance: Double, max: Double, avg: Double,
	                                   stdDev: Double, highToLow: Boolean, min: Double, volume: Long,
		                                 fivePercent: Double, wavg: Double, timeat: DateTime) extends OrderStatistics
}


class GetHistStats extends Actor {

	def receive = {
		case GetHistStats.Request(mtype, bid, region, system, from, to) => {
			import net.noerd.prequel.SQLFormatterImplicits._
			val regionid = region match { case Left(ar: AnyRegion) => -1 case Right(region) => region.regionid }
			val systemid = system match { case Some(s) => s.systemid case None => 0 }
			val bidint = if (bid) 1 else 0
			sender ! Database.coreDb.transaction { tx =>
					tx.select("SELECT average,median,volume,stddev,buyup,minimum,maximum,timeat FROM trends_type_region WHERE typeid = ? AND " +
						"systemid = ? AND regionid = ? AND bid = ? AND timeat > ? AND timeat < ? ORDER BY timeat",
						mtype.typeid, systemid, regionid, bidint, from, to) { row =>
						val avg = row.nextDouble.get
						val median = row.nextDouble.get
						val volume = row.nextLong.get
						val stddev = row.nextDouble.get
						val buyup = row.nextDouble.get
						val minimum = row.nextDouble.get
						val maximum = row.nextDouble.get
						val timeat = new DateTime(row.nextDate.get)
						GetHistStats.CapturedOrderStatistics(median, 0, maximum, avg, stddev, bid, minimum, volume, buyup, 0, timeat)
					}

			}
		}

	}

}
