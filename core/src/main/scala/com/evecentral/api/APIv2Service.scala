package com.evecentral.api

import cc.spray.http.MediaTypes._
import akka.actor.{PoisonPill, Actor, Scheduler}
import cc.spray.{Pass, Directives}
import cc.spray.http._


import org.parboiled.scala._
import java.net.URI
import java.net.URLDecoder

import org.parboiled.scala._
import org.parboiled.errors.ErrorUtils
import scala.xml._
import scala.collection.breakOut
import com.evecentral.dataaccess._


private[api] trait BaseParser extends Parser {

  def parse[A](rule: Rule1[A], input: String): Either[String, A] = {
    val result = ReportingParseRunner(rule).run(input)
    result.result match {
      case Some(value) => Right(value)
      case None => Left(ErrorUtils.printParseErrors(result))
    }
  }

}

object RepeatQueryParser extends BaseParser {

  def QueryString: Rule1[List[(String, String)]] = rule(
    EOI ~ push(List())
      | zeroOrMore(QueryParameter, separator = "&") ~ EOI ~~> (_.toList)
  )

  def QueryParameter = rule {
    QueryParameterComponent ~ optional("=") ~ (QueryParameterComponent | push(""))
  }

  def QueryParameterComponent = rule {
    zeroOrMore(!anyOf("&=") ~ ANY) ~> (s => URLDecoder.decode(s, "UTF8"))
  }

  def parse(queryString: String): List[(String, String)] = {
    try {
      parse(QueryString, queryString) match {
        case Left(error) => throw new RuntimeException(error)
        case Right(parameterMap) => parameterMap
      }
    } catch {
      case e: Exception => throw new HttpException(StatusCodes.BadRequest,
        "Illegal query string '" + queryString + "':\n" + e.getMessage)
    }
  }

}

trait APIv2Service extends Directives {

  def extractListOfParams(uri: String): List[(String, String)] = {
    RepeatQueryParser.parse(new URI(uri).getRawQuery)
  }

  def ordersActor = {
    val r = (Actor.registry.actorsFor[GetOrdersActor]);
    r(0)
  }

  def paramsFromQuery(name: String, params: List[(String, String)]): List[String] = {
    params.foldLeft(List[String]()) {
      (i, s) => if (s._1 == name) s._2 :: i else i
    }
  }

  def singleParam[T](name: String, params: List[(String, String)]): Option[Long] = {
    paramsFromQuery(name, params) match {
      case Nil => None
      case x: List[String] => Some(x(0).toLong)
    }
  }

  def regionName(regions: List[Long]): NodeSeq = {
    regions.foldLeft(Seq[Node]()) {
      (i, regionid) =>
        i ++ <region>
          {StaticProvider.regionsMap(regionid)}
        </region>
    }
  }

  def queryQuicklook(typeid: Long, setHours: Long, regionLimit: List[Long],
                     usesystem: Option[Long], qminq: Option[Long]): NodeSeq = {

    val minq = qminq match {
      case Some(x) => x
      case None => QueryDefaults.minQ(typeid)
    }

    val buyq = GetOrdersFor(true, List(typeid), regionLimit, usesystem match {
      case None => Nil
      case Some(x) => List[Long](x)
    }, setHours)
    val selq = GetOrdersFor(false, List(typeid), regionLimit, usesystem match {
      case None => Nil
      case Some(x) => List[Long](x)
    }, setHours)

    val buyr = ordersActor ? buyq
    val selr = ordersActor ? selq

    <evec_api version="2.0" method="quicklook">
      <quicklook>
        <item>
          {typeid}
        </item>
        <itemname>
          {StaticProvider.typesMap(typeid)}
        </itemname>
        <regions>
          {regionName(regionLimit)}
        </regions>
        <hours>
          {setHours}
        </hours>
        <minqty>
          {minq}
        </minqty>
      </quicklook>
    </evec_api>
  }

  val v2Service = {
    path("api/quicklook") {
      get {
        detach {
          respondWithContentType(`text/xml`) {
            ctx =>
              val params = extractListOfParams(ctx.request.uri)
              val typeid = singleParam("typeid", params) match {
                case Some(x) => x
                case None => 34
              }
              val setHours = singleParam("sethours", params) match {
                case Some(x) => x
                case None => 24
              }
              val regionLimit = paramsFromQuery("regionlimit", params).map(_.toLong).distinct
              val usesystem = singleParam("usesystem", params)
              val minq = singleParam("setminQ", params) // We need more logic for single param
              ctx.complete(queryQuicklook(typeid, setHours, regionLimit, usesystem, minq))

          }
        }
      }
    }
  }

}


