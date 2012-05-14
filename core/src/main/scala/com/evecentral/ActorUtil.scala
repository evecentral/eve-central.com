package com.evecentral


import akka.dispatch.Future
import akka.routing._
import akka.actor.{Actor, Channel}

trait ECActorPool extends Actor with DefaultActorPool
with FixedCapacityStrategy
with RoundRobinSelector
with BasicNoBackoffFilter {

  def receive = _route

  def limit = 4

  def partialFill = true

  def rampupRate = 1

  def selectionCount = 1 
}
