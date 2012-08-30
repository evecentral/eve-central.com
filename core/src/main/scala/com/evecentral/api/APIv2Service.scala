package com.evecentral.api

import cc.spray.http.MediaTypes._
import cc.spray.directives.{Remaining, IntNumber}
import cc.spray.{RequestContext, Directives}
import cc.spray.typeconversion.LiftJsonSupport

import org.slf4j.LoggerFactory
import org.joda.time.DateTime

import com.codahale.jerkson.Json._
import net.liftweb.json._

import akka.actor.Actor
import Actor.actorOf

import scala.xml._

import com.evecentral.dataaccess._
import com.evecentral.ParameterHelper._
import com.evecentral.frontend.Formatter.priceString
import com.evecentral._
import datainput.{OldUploadParsingActor, OldUploadPayload}
import frontend.DateFormats
import routes.{Jump, RouteBetween}
import util.BaseOrderQuery
import dataaccess.OrderList
import akka.dispatch.Future


case class QuickLookSimpleQuery(ctx: RequestContext)
case class QuickLookPathQuery(ctx: RequestContext, from: SolarSystem, to: SolarSystem, types: Int)

class QuickLookQuery extends Actor with FixedSprayMarshallers with BaseOrderQuery {

	import com.evecentral.ParameterHelper._

	def receive = {
		case QuickLookPathQuery(ctx, froms, tos, types) =>

			val params = listFromContext(ctx)

			val setHours = singleParam("sethours", params) match {
				case Some(x) => x
				case None => 360
			}
			val minq = singleParam("setminQ", params)
			ctx.complete(queryQuicklookPath(types, setHours, minq, froms, tos))

		case QuickLookSimpleQuery(ctx) =>

			val params = listFromContext(ctx)

			val typeid = singleParam("typeid", params) match {
				case Some(x) => x
				case None => 34
			}
			val setHours = singleParam("sethours", params) match {
				case Some(x) => x
				case None => 360
			}
			val regionLimit = paramsFromQuery("regionlimit", params).map(_.toLong).distinct
			val usesystem = singleParam("usesystem", params)
			val minq = singleParam("setminQ", params)
			ctx.complete(queryQuicklook(typeid, setHours, regionLimit, usesystem, minq))

	}

	def regionName(regions: List[Long]): NodeSeq = {
		regions.foldLeft(Seq[Node]()) {
			(i, regionid) =>
				i ++ <region>{StaticProvider.regionsMap(regionid).name}</region>
		}
	}

	def showOrders(orders: Option[OrderList]): NodeSeq = {

		orders match {
			case None => Seq[Node]()
			case Some(o) => o.result.foldLeft(Seq[Node]()) {
				(i, order) =>
					i ++ <order id={order.orderId.toString}>
						<region>{order.region.regionid}</region>
						<station>{order.station.stationid}</station>
						<station_name>{order.station.name}</station_name>
						<security>{order.system.security}</security>
						<range>{order.range}</range>
						<price>{priceString(order.price)}</price>
						<vol_remain>{order.volremain}</vol_remain>
						<min_volume>{order.minVolume}</min_volume>
						<expires>{DateFormats.dateOnly.print(new DateTime().plus(order.expires))}</expires>
						<reported_time>{DateFormats.dateTime.print(order.reportedAt)}</reported_time>
					</order>
			}
		}
	}

	def queryQuicklookPath(typeid: Long, setHours: Long, qminq: Option[Long], froms: SolarSystem, tos: SolarSystem) : NodeSeq = {

		val minq = qminq match {
			case Some(x) => x
			case None => QueryDefaults.minQ(typeid)
		}

		val path = (pathActor ? RouteBetween(froms, tos)).as[Seq[Jump]] match {
			case Some(jumps) =>
				jumps
			case None => List[Jump]()
		}

		val systems = path.foldLeft(Set[SolarSystem]()) { (set, j) => set + j.from + j.to }.toList.map(_.systemid)

		val buyq = GetOrdersFor(Some(true), List(typeid), List(), systems, setHours)
		val selq = GetOrdersFor(Some(false), List(typeid), List(), systems, setHours)

		val buyr = ordersActor ? buyq
		val selr = ordersActor ? selq

		<evec_api version="2.0" method="quicklook_path">
			<quicklook>
				<item>{typeid}</item>
				<itemname>{StaticProvider.typesMap(typeid).name}</itemname>
				<regions></regions>
				<hours>{setHours}</hours>
				<minqty>{minq}</minqty>
				<sell_orders>{showOrders(selr.as[OrderList])}</sell_orders>
				<buy_orders>{showOrders(buyr.as[OrderList])}</buy_orders>
				<from>{froms.systemid}</from>
				<to>{tos.systemid}</to>
			</quicklook>
		</evec_api>
	}

	def queryQuicklook(typeid: Long, setHours: Long, regionLimit: List[Long],
	                   usesystem: Option[Long], qminq: Option[Long]): NodeSeq = {

		val minq = qminq match {
			case Some(x) => x
			case None => QueryDefaults.minQ(typeid)
		}

		val buyq = GetOrdersFor(Some(true), List(typeid), regionLimit, usesystem match {
			case None => Nil
			case Some(x) => List[Long](x)
		}, setHours)
		val selq = GetOrdersFor(Some(false), List(typeid), regionLimit, usesystem match {
			case None => Nil
			case Some(x) => List[Long](x)
		}, setHours)

		val buyr = ordersActor ? buyq
		val selr = ordersActor ? selq

		<evec_api version="2.0" method="quicklook">
			<quicklook>
				<item>{typeid}</item>
				<itemname>{StaticProvider.typesMap(typeid).name}</itemname>
				<regions>{regionName(regionLimit)}</regions>
				<hours>{setHours}</hours>
				<minqty>{minq}</minqty>
				<sell_orders>{showOrders(selr.as[OrderList])}</sell_orders>
				<buy_orders>{showOrders(buyr.as[OrderList])}</buy_orders>
			</quicklook>
		</evec_api>
	}

}

case class MarketstatQuery(ctx: RequestContext, dtype: String = "xml")
case class EvemonQuery(ctx: RequestContext)

class MarketStatActor extends ECActorPool with FixedSprayMarshallers with LiftJsonSupport with BaseOrderQuery {

	private val log = LoggerFactory.getLogger(getClass)
	val liftJsonFormats = DefaultFormats

	/* This is for the Akka 1.3 pool of actors */

	def instance = { actorOf(new Actor {
		def receive = {

			case EvemonQuery(ctx) =>
				val types = List(34, 35, 36, 37, 38, 39, 40, 11399).map(StaticProvider.typesMap(_))

				ctx.complete(<minerals>
					{types.map(evemonMineral(_))}
				</minerals>)
			case MarketstatQuery(ctx, dtype) =>
				try {
					val params = listFromContext(ctx)
					val typeid = paramsFromQuery("typeid", params).map(_.toLong).distinct

					val setHours = singleParam("hours", params) match {
						case Some(x) => x
						case None => 24
					}
					val regionLimitListStrings = paramsFromQuery("regionlimit", params)
					val regionLimit = if (regionLimitListStrings.size > 1) {
						regionLimitListStrings.map(_.toLong).distinct
					} else if (regionLimitListStrings.size == 1) {
						regionLimitListStrings(0).split(",").toList.filter(_.size > 0).map(_.toLong).distinct // Come up with a list of regionlimits comma seperated
					} else {
						Seq[Long]()
					}
					val usesystem = singleParam("usesystem", params)
					val minq = singleParam("minQ", params)

					if (dtype == "json") {
						ctx.complete(wrapAsJson(typeid.map(t => getCachedStatistics(t, setHours, regionLimit, usesystem, minq))))
					} else {
						ctx.complete(wrapAsXml(typeid.map(t => typeXml(getCachedStatistics(t, setHours, regionLimit, usesystem, minq), t))))
					}


				} catch {
					case t : Throwable => ctx.fail(t)
				}
		}


		def evemonMineral(mineral: MarketType) : NodeSeq = {
			val buyq = GetOrdersFor(None, List(mineral.typeid), StaticProvider.empireRegions.map(_.regionid), Nil)
			val s = fetchCachedStats(buyq, true) getOrElse storeCachedStats(OrderStatistics(fetchOrdersFor(buyq), true), buyq)

			<mineral>
				<name>{mineral.name}</name>
				<price>{priceString(s.wavg)}</price>
			</mineral>
		}

		/* Produce an XML document of all statistics */
		def typeXml(r: (OrderStatistics, OrderStatistics, OrderStatistics), typeid: Long) : NodeSeq = {

			def subGroupXml(alls: OrderStatistics) : NodeSeq = {
				<volume>{alls.volume}</volume>
					<avg>{priceString(alls.wavg)}</avg>
					<max>{priceString(alls.max)}</max>
					<min>{priceString(alls.min)}</min>
					<stddev>{priceString(alls.stdDev)}</stddev>
					<median>{priceString(alls.median)}</median>
					<percentile>{priceString(alls.fivePercent)}</percentile>
			}

			val buys: OrderStatistics = r._1
			val alls: OrderStatistics = r._2
			val sels: OrderStatistics = r._3

			<type id={typeid.toString}>
				<buy>
					{subGroupXml(buys)}
				</buy>
				<sell>
					{subGroupXml(sels)}
				</sell>
				<all>
					{subGroupXml(alls)}
				</all>
			</type>
		}


		def wrapAsXml(nodes: Seq[NodeSeq]) = <evec_api version="2.0" method="marketstat_xml">
			<marketstat>
				{nodes}
			</marketstat>
		</evec_api>

		case class BuyAllSell(buy: OrderStatistics, all: OrderStatistics, sell: OrderStatistics)

		def wrapAsJson(types: Seq[(OrderStatistics, OrderStatistics, OrderStatistics)]) : String = generate(types.map(u => BuyAllSell(u._1, u._2, u._3)))

	})}

}
///////////////////////////////////////////////////////////////////////////////////






//////////////////////////////////////////////////////////////////////////////////////////////
trait APIv2Service extends Directives {

	val quicklookActor = actorOf(new QuickLookQuery())
	val marketstatActor = actorOf(new MarketStatActor())
	val olduploadActor = actorOf(new OldUploadParsingActor())

	import LookupHelper._

	val v2Service = {
		path("api/quicklook/onpath/from" / "[^/]+".r / "to" / "[^/]+".r / "fortype" / IntNumber) {
			(fromr, tor, types) =>
				val fromid = lookupSystem(fromr)
				val toid = lookupSystem(tor)
				(get | post) {
					ctx =>
						quicklookActor ! QuickLookPathQuery(ctx, fromid, toid, types)
				}
		} ~
			path("api/quicklook") {

				(get | post) {
					ctx =>
						(quicklookActor ! QuickLookSimpleQuery(ctx))

				}
			} ~ path("api/marketstat" / Remaining) {
			dtype =>
				(get | post) {
					ctx =>
						if (dtype.size > 0)
							(marketstatActor ! MarketstatQuery(ctx, dtype))
						else
							(marketstatActor ! MarketstatQuery(ctx))
				}
		} ~ path("api/evemon") {
			(get | post) {
				ctx =>
					(marketstatActor ! EvemonQuery(ctx))
			}
		} ~ path("datainput.py/inputdata") {
			post {
				formFields("typename"?, "userid"?, "data", "typeid"?, "region"?) {
					(typename, userid, data, typeid, region) =>
						olduploadActor ! OldUploadPayload(_, typename, userid, data, typeid, region)
				}

			}
		}
	}
}



