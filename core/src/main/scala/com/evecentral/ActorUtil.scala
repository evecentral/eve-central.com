package com.evecentral


import akka.dispatch.Future
import akka.routing._
import akka.actor.{Actor, Channel}
import akka.config.Supervision.OneForOneStrategy

trait ECActorPool extends Actor with DefaultActorPool
with FixedCapacityStrategy
with RoundRobinSelector
with BasicNoBackoffFilter {

  def receive = _route

  def limit = 4

  def partialFill = true

  def rampupRate = 1

  def selectionCount = 1

	self.faultHandler = OneForOneStrategy(List(classOf[Throwable]), 100, 1000)

}
