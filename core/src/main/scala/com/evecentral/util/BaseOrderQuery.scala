package com.evecentral.util

import akka.actor.Actor
import com.evecentral.dataaccess.GetOrdersActor
import com.evecentral.OrderCacheActor
import com.evecentral.routes.RouteFinderActor


trait BaseOrderQuery {

	def ordersActor = {
		val r = Actor.registry.actorsFor[GetOrdersActor]
		r(0)
	}

	def statCache = {
		val r = (Actor.registry.actorsFor[OrderCacheActor]);
		r(0)
	}

	def pathActor = {
		val r = (Actor.registry.actorsFor[RouteFinderActor]); r(0)
	}

}
