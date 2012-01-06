package com.evecentral


import akka.dispatch.Future
import akka.routing._
import akka.actor.{Actor, Channel}


trait ActorUtil {
  def pipeTo(c: Channel[Any]): Future[Any] => Unit =
    (f: Future[Any]) => f.value.get.fold(c.sendException(_),c.tell(_))
}

trait ECActorPool extends Actor with DefaultActorPool with BoundedCapacityStrategy
with ActiveFuturesPressureCapacitor
with SmallestMailboxSelector
with BasicNoBackoffFilter {

  def receive = _route

  def lowerBound = 4

  def upperBound = 8

  def rampupRate = 0.1

  def partialFill = true

  def selectionCount = 1 
}
