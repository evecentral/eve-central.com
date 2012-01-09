package com.evecentral

import cc.spray._

/**
 * A very simple service to publish anything in resources/static
 * to the URL /static.
 * These static files are intended to be CDN hosted in production.
 */
trait StaticService extends Directives {
  val staticService = {
    pathPrefix("static") {
      getFromResourceDirectory("static")
    }
  }
  
}
