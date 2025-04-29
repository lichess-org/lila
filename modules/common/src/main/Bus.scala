package lila.common

import akka.actor.{ ActorRef, Scheduler }
import scalalib.bus.{ Channel, Tellable }

val Bus = scalalib.bus.Bus(initialCapacity = 4096)

// TODO FIXME migrate to new api and stop using the same method name for so many different input
object actorBus:

  case class ActorTellable(ref: ActorRef) extends Tellable:
    def !(msg: Matchable) = ref ! msg

  extension (bus: scalalib.bus.Bus)

    // LOGIC : It is up to the caller to make sure `T`'s channel is relevant to the `tellable`
    inline def subscribeActorRef[T <: scalalib.bus.Bus.Payload](ref: ActorRef) =
      Bus.subTellable[T](ActorTellable(ref))

    // LOGIC : It is up to the caller to make sure `T`'s channel is relevant to the `tellable`
    inline def unsubscribeActorRef[T <: scalalib.bus.Bus.Payload](ref: ActorRef) =
      Bus.unsub[T](ActorTellable(ref))

    def subscribeActorRefDynamic(ref: ActorRef, to: Channel*): Unit =
      to.foreach(Bus.subscribe(ActorTellable(ref), _))

    def subscribeActorRefDynamic(ref: ActorRef, to: Iterable[Channel]) =
      to.foreach(Bus.subscribe(ActorTellable(ref), _))

    def unsubscribeActorRefDynamic(ref: ActorRef, from: Channel) =
      Bus.unsubscribe(ActorTellable(ref), from)
