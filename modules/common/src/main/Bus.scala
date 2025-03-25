package lila.common

import akka.actor.{ ActorRef, Scheduler }
import scalalib.bus.{ Channel, Tellable }

val Bus = scalalib.bus.Bus(initialCapacity = 4096)

object actorBus:

  case class ActorTellable(ref: ActorRef) extends Tellable:
    def !(msg: Matchable) = ref ! msg

  extension (bus: scalalib.bus.Bus)

    def subscribe(ref: ActorRef, to: Channel*): Unit =
      to.foreach(Bus.subscribe(ActorTellable(ref), _))

    def subscribe(ref: ActorRef, to: Iterable[Channel]) =
      to.foreach(Bus.subscribe(ActorTellable(ref), _))

    def unsubscribe(ref: ActorRef, from: Channel) =
      Bus.unsubscribe(ActorTellable(ref), from)
