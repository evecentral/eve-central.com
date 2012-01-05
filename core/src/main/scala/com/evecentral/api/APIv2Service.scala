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

  def QueryString: Rule1[List[(String, String)]] = rule (
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

  def extractListOfParams(uri: String): List[(String,String)] = {
    RepeatQueryParser.parse(new URI(uri).getRawQuery)
  }

  def getOrders = {
    val r = (Actor.registry.actorsFor[GetOrdersActor]);
    r(0)
  }

  val v2Service = {
    path("api/orders") {
      get {

          respondWithContentType(`text/xml`) {
              ctx =>
              val params = extractListOfParams(ctx.request.uri)
              ctx.complete(<xml>
                {params.toString}
              </xml>)

          }
      }
    }
  }

}


