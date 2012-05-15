package com.evecentral.datainput

import akka.actor.Actor
import org.slf4j.LoggerFactory
import com.evecentral.util.BaseOrderQuery
import akka.dispatch.Future
import com.evecentral.dataaccess.{MarketOrder, OrderList, StaticProvider, GetOrdersFor}
import net.noerd.prequel.{Formattable, IntFormattable}
import com.evecentral.{RegisterCacheFor, OrderStatistics, Database}

class StatisticsCaptureActor extends Actor with BaseOrderQuery {

	private val log = LoggerFactory.getLogger(getClass)

	override def preStart() {
		log.info("Pre-starting statistics capture actor")
	}

	def buildQueries(bid: Boolean, typeid: Int, regionid: Long) : List[GetOrdersFor] = {
		val base = List(GetOrdersFor(Some(bid), List(typeid), List(regionid), List(), 1),
			GetOrdersFor(Some(bid), List(typeid), List(), List(), 24),
			GetOrdersFor(Some(bid), List(typeid), StaticProvider.empireRegions.map(_.regionid), List(), 24))

		if (regionid == StaticProvider.regionsByName("The Forge").regionid)
			base ++ List(GetOrdersFor(Some(bid), List(typeid), List(), List(StaticProvider.systemsByName("Jita").systemid), 24))
		else
			base

	}

	def storeStatistics(query: GetOrdersFor, result: OrderStatistics) {
		val region = if (query.regions.size > 1) -1 else if (query.regions.size == 1) query.regions.head else 0
		val system = query.systems.headOption match {
			case Some(y) => y.toLong
			case None => 0
		}
		val item = query.types(0).toInt


		Database.coreDb.transaction {
			tx =>
				import net.noerd.prequel.SQLFormatterImplicits._
				tx.execute("INSERT INTO trends_type_region (typeid, region, average, median, volume, stddev, buyup, systemid, bid, minimum, maximum) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
					item,
					region, result.avg, result.median, result.volume, result.stdDev, result.fivePercent, system,
					query.bid.get match { case true => 1 case false => 0 }, result.min, result.max)
		}

		val cached = OrderStatistics.cached(query, result)
		statCache ! RegisterCacheFor(cached)
	}


	def receive = {
		case UploadTriggerEvent(typeid, regionid) =>
			/* Build queries for orders */
			log.debug("Generating list of queries to get results for")
			val queries = buildQueries(true, typeid, regionid).map(ordersActor ? _) ++ buildQueries(false, typeid, regionid).map(ordersActor ? _)
		  queries.foreach(v => v.get match { case OrderList(query, result) => storeStatistics(query, OrderStatistics(result, query.bid.getOrElse(false))) } )

	}

}
