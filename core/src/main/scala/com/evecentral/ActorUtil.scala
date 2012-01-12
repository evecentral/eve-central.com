package com.evecentral


import akka.dispatch.Future
import akka.routing._
import akka.actor.{Actor, Channel}


trait ActorUtil {
  def pipeTo(c: Channel[Any]): Future[Any] => Unit =
    (f: Future[Any]) => f.value.get.fold(c.sendException(_),c.tell(_))
}

trait ECActorPool extends Actor with DefaultActorPool
with FixedCapacityStrategy
with SmallestMailboxSelector
with BasicNoBackoffFilter {

  def receive = _route

  def limit = 4

  def partialFill = true

  def rampupRate = 1

  def selectionCount = 1 
}
