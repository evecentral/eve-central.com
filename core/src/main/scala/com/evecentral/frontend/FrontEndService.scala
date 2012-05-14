package com.evecentral.frontend

import cc.spray.http.MediaTypes._
import java.util.concurrent.TimeUnit
import akka.actor.{PoisonPill, Actor, Scheduler}
import cc.spray.Directives
import cc.spray.ScalateSupport
import cc.spray.directives.Remaining

trait FrontEndService extends Directives with ScalateSupport {

  val frontEndService = {
	  path("") {
		  get {
			  render("com/evecentral/templates/index.ssp", Map())
		  }
	  } ~
		  path("plot") {
			  get {
				  render("com/evecentral/templates/plottest.ssp", Map())
			  }
		  } ~
		  path("orders" / Remaining) {
			  remain =>
				  get {
					  render("com/evecentral/templates/orders.ssp", Map())
				  }
		  }

  }

}
