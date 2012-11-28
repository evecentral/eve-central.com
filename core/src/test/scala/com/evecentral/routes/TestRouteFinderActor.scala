package com.evecentral.routes

import org.scalatest.{BeforeAndAfterAll, FunSuite}
import akka.actor.{ActorSystem, Actor}
import com.evecentral.dataaccess.StaticProvider
import akka.testkit.{TestKit, TestActorRef}

class RouteFinderTest(as: ActorSystem) extends TestKit(as) with FunSuite with BeforeAndAfterAll {

	def this() = this(ActorSystem("MySpec"))

	val rfa = TestActorRef[RouteFinderActor]
	val rf = rfa.underlyingActor

	val jita = StaticProvider.systemsMap(30000142)
	val sagain = StaticProvider.systemsMap(30001719)
	val perimiter = StaticProvider.systemsMap(30000144)

	test("Jita to Sagain distance") {
		assert(rf.routeDistance(jita, sagain) == 15)
	}

	test("Jita to Sagain route") {
		val route = rf.route(jita, sagain)
		val expected_route_contains = Jump(jita, perimiter)
		assert(route contains expected_route_contains, "Got %s" format (route))
	}

	test("Actor Jita to Sagain distance") {
		(rfa ! DistanceBetween(jita, sagain))
		expectMsg(15)
	}

}
