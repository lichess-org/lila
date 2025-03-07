package lila.common

import akka.actor.{ ActorRef, Scheduler }
import scalalib.bus.{ Channel, Tellable }

val Bus = scalalib.bus.Bus(initialCapacity = 4096)

object actorBus:

  extension (bus: scalalib.bus.Bus)

    def subscribe(ref: ActorRef, to: Channel*): Unit =
      to.foreach(Bus.subscribe(actorTellable(ref), _))

    def subscribe(ref: ActorRef, to: Iterable[Channel]) =
      to.foreach(Bus.subscribe(actorTellable(ref), _))

    def unsubscribe(ref: ActorRef, from: Channel) = Bus.unsubscribe(actorTellable(ref), from)

  private def actorTellable(ref: ActorRef): Tellable = new:
    def !(msg: Matchable) = ref ! msg
