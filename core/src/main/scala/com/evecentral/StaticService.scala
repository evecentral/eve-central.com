package com.evecentral

import cc.spray._

trait StaticService extends Directives {
  val staticService = {
    pathPrefix("static") {
      getFromResourceDirectory("static")
    }
  }
  
}
