package com.evecentral

import cc.spray.http._

import java.net.URI
import java.net.URLDecoder

import org.parboiled.scala._
import org.parboiled.errors.ErrorUtils

object ParameterHelper {
  def paramsFromQuery(name: String, params: List[(String, String)]): List[String] = {
    params.foldLeft(List[String]()) {
      (i, s) => if (s._1 == name) s._2 :: i else i
    }
  }

  def extractListOfParams(uri: String): List[(String, String)] = {
    RepeatQueryParser.parse(new URI(uri).getRawQuery)
  }

  def singleParam[T](name: String, params: List[(String, String)]): Option[Long] = {
    paramsFromQuery(name, params) match {
      case Nil => None
      case x: List[String] => Some(x(0).toLong)
    }
  }

}


private[evecentral] trait BaseParser extends Parser {

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

