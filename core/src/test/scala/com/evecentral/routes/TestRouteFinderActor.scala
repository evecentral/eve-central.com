package com.evecentral.routes

import org.scalatest.FunSuite
import akka.actor.{ActorSystem, Actor}
import com.evecentral.dataaccess.StaticProvider
import akka.testkit.{TestKit, TestActorRef, TestActor}
import com.typesafe.config.ConfigFactory

object RouteFinderTest {
	implicit val system = ActorSystem("testsystem", ConfigFactory.parseString("""
  akka.event-handlers = ["akka.testkit.TestEventListener"] """))
}

class RouteFinderTest extends TestKit(RouteFinderTest.system) with FunSuite {


  val rfa = TestActorRef(new RouteFinderActor)
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
