package com.evecentral.datainput

import akka.actor.{Actor, Props}
import akka.util.duration._
import org.slf4j.LoggerFactory
import com.evecentral.util.BaseOrderQuery
import akka.util.duration._
import akka.pattern.ask

import com.evecentral.dataaccess.{OrderList, StaticProvider, GetOrdersFor}
import com.evecentral.{RegisterCacheFor, OrderStatistics, Database}
import akka.dispatch.Future

private[this] case class CaptureStatistics()
private[this] case class StoreStatistics(query: GetOrdersFor, result: OrderStatistics)

class StatisticsCaptureActor extends Actor with BaseOrderQuery {

	import context.dispatcher

	private val log = LoggerFactory.getLogger(getClass)
	private val toCaptureSet = scala.collection.mutable.Set[GetOrdersFor]()

	override def preStart() {
		log.info("Pre-starting statistics capture actor")
		context.system.scheduler.schedule(2.minutes, 60.minutes, self, CaptureStatistics())
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
			if (StaticProvider.typesMap.contains(typeid))
				toCaptureSet ++= (buildQueries(true, typeid, regionid) ++ buildQueries(false, typeid, regionid))
		case StoreStatistics(query, result) =>
			storeStatistics(query, result)
		case CaptureStatistics() =>
			log.info("Capturing statistics in a large batch")
			val results = toCaptureSet.toList.map(capset => (ordersActor ? capset).mapTo[OrderList])
			// Attach an oncomplete to all the actors
			results.map { entity =>
				entity onSuccess {
					case OrderList(query, result) => self ! StoreStatistics(query, OrderStatistics(result, query.bid.getOrElse(false)))
				}
			}
			log.info(results.size + " results to capture")
			toCaptureSet.clear()

	}

}
