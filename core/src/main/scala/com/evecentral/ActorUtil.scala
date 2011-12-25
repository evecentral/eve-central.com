package com.evecentral


import akka.dispatch.Future
import akka.actor.Channel

trait ActorUtil {
  def pipeTo(c: Channel[Any]): Future[Any] => Unit =
    (f: Future[Any]) => f.value.get.fold(c.sendException(_),c.tell(_))
}
