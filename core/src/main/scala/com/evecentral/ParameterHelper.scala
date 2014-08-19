package com.evecentral

import org.slf4j.{LoggerFactory, Logger}
import spray.http.HttpEntity.Empty
import spray.http._

import org.parboiled.scala._
import org.parboiled.errors.{ErrorUtils}
import spray.routing.RequestContext
import spray.httpx.marshalling.{Marshaller, BasicMarshallers}

import xml.NodeSeq
import spray.http.MediaTypes._

/**
 * Attach the xml header to a nodeseq
 */
trait FixedSprayMarshallers extends BasicMarshallers {
  override implicit val NodeSeqMarshaller: Marshaller[NodeSeq] =
    Marshaller.delegate[NodeSeq, String](
      `text/xml`, `text/html`, `application/xhtml+xml`
    ) {
      nodes: NodeSeq => "<?xml version='1.0' encoding='utf-8'?>\n" + nodes.toString
    }
}

/**
 * A helper object for dealing with parameter lists, especially ones
 * containing repeated parameters which don't play nice with spray's
 * single-map implementation of them.
 */
object ParameterHelper {

  private[this] val log = LoggerFactory.getLogger(getClass)

  def paramsFromQuery(name: String, params: Uri.Query): List[String] = {
    (params.foldLeft(List[String]()) {
      (i, s) => if (s._1 == name) s._2 :: i else i
    }).reverse
  }


  def singleParam[T](name: String, params: Uri.Query): Option[Long] = {
    params.get(name).map { _.toLong }
  }

  def singleStringParam[T](name: String, params: Uri.Query): Option[String] = {
    params.get(name)
  }

  def listFromContext(ctx: RequestContext): Uri.Query = {
    try {
      val formdata = ctx.request.entity.data match {
        case HttpData.Empty => None
        case t: HttpData => Some(t.asString(HttpCharsets.`UTF-8`))
      }
      formdata match {
        case None => ctx.request.uri.query
        case Some(fd) => {
          val q = Uri.Query(ctx.request.uri.render(new StringRendering).get + "?" + fd)
          log.info("QUERY: " + q)
          q
        }
      }
    } catch {
      case _ : Throwable => Uri.Query(None)
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

