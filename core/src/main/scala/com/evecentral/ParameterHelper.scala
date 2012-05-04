package com.evecentral

import cc.spray.http._

import java.net.URI
import java.net.URLDecoder

import org.parboiled.scala._
import org.parboiled.errors.ErrorUtils
import cc.spray.RequestContext
import java.nio.charset.Charset

/**
 * A helper object for dealing with parameter lists, especially ones
 * containing repeated parameters which don't play nice with spray's
 * single-map implementation of them.
 */
object ParameterHelper {
  
  type ML = List[(String,  String)]
  
  def paramsFromQuery(name: String, params: ML): List[String] = {
    (params.foldLeft(List[String]()) {
      (i, s) => if (s._1 == name) s._2 :: i else i
    }).reverse
  }

  def extractListOfParams(uri: String): ML = {
    RepeatQueryParser.parse(new URI(uri).getRawQuery)
  }

  def singleParam[T](name: String, params: ML): Option[Long] = {
    paramsFromQuery(name, params) match {
      case Nil => None
      case x: List[String] => Some(x(0).toLong)
    }
  }

  def singleStringParam[T](name: String, params: ML): Option[String] = {
    paramsFromQuery(name, params) match {
      case Nil => None
      case x: List[String] => Some(x(0))
    }
  }

  def listFromContext(ctx : RequestContext) : ML = {
    val formdata = ctx.request.content match {
      case Some(c) => Some(new String(c.buffer, Charset.forName("UTF-8")))
      case None => None
    }

    formdata match {
      case None => extractListOfParams(ctx.request.uri)
      case Some(fd) => extractListOfParams(ctx.request.uri + "?" + fd)
    }
  }
  
}

/**
 * Either we use reflection shenanigans to get to BaseParser in spray,
 * or we just duplicate it here :-/
 */
private[evecentral] trait BaseParser extends Parser {

  def parse[A](rule: Rule1[A], input: String): Either[String, A] = {
    val result = ReportingParseRunner(rule).run(input)
    result.result match {
      case Some(value) => Right(value)
      case None => Left(ErrorUtils.printParseErrors(result))
    }
  }

}

/**
 * Shamelessly stolen and adapted from spray-base.
 * Parses query lists with repeated parameters - an old legacy from EC
 * (but something that cherrypy did out of the box)
 */
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

