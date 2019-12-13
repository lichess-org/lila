package lila.common

import scala.concurrent.duration._
import scala.concurrent.Promise

import akka.actor.{ ActorRef, ActorSystem }

object Bus {

  case class Event(payload: Any, channel: String)
  type Channel    = String
  type Subscriber = Tellable

  def publish(payload: Any, channel: Channel): Unit = {
    publish(Bus.Event(payload, channel))
  }

  def subscribe = bus.subscribe _

  def subscribe(ref: ActorRef, to: Channel) = bus.subscribe(Tellable(ref), to)

  def subscribe(subscriber: Tellable, to: Channel*)   = to foreach { bus.subscribe(subscriber, _) }
  def subscribe(ref: ActorRef, to: Channel*)          = to foreach { bus.subscribe(Tellable(ref), _) }
  def subscribe(ref: ActorRef, to: Iterable[Channel]) = to foreach { bus.subscribe(Tellable(ref), _) }

  def subscribeFun(to: Channel*)(f: PartialFunction[Any, Unit]): Tellable = {
    val t = lila.common.Tellable(f)
    subscribe(t, to: _*)
    t
  }

  def subscribeFuns(subscriptions: (Channel, PartialFunction[Any, Unit])*): Unit =
    subscriptions foreach {
      case (channel, subscriber) => subscribeFun(channel)(subscriber)
    }

  def unsubscribe                               = bus.unsubscribe _
  def unsubscribe(ref: ActorRef, from: Channel) = bus.unsubscribe(Tellable(ref), from)

  def unsubscribe(subscriber: Tellable, from: Iterable[Channel]) = from foreach {
    bus.unsubscribe(subscriber, _)
  }
  def unsubscribe(ref: ActorRef, from: Iterable[Channel]) = from foreach { bus.unsubscribe(Tellable(ref), _) }

  def publish(event: Event): Unit = bus.publish(event.payload, event.channel)

  def ask[A](channel: Channel, timeout: FiniteDuration = 1.second)(makeMsg: Promise[A] => Any)(
      implicit
      system: ActorSystem
  ): Fu[A] = {
    val promise = Promise[A]
    val msg     = makeMsg(promise)
    publish(msg, channel)
    promise.future.withTimeout(
      timeout,
      Bus.AskTimeout(s"Bus.ask timeout: $channel $msg")
    )
  }

  private val bus = new EventBus[Any, Channel, Tellable](
    initialCapacity = 65535,
    publish = (tellable, event) => tellable ! event
  )

  def size = bus.size

  case class AskTimeout(message: String) extends lila.base.LilaException
}

final private class EventBus[E, C, Subscriber](
    initialCapacity: Int,
    publish: (Subscriber, E) => Unit
) {

  import java.util.concurrent.ConcurrentHashMap

  private val entries = new ConcurrentHashMap[C, Set[Subscriber]](initialCapacity)

  def subscribe(subscriber: Subscriber, channel: C): Unit =
    entries.compute(channel, (_: C, subs: Set[Subscriber]) => {
      Option(subs).fold(Set(subscriber))(_ + subscriber)
    })

  def unsubscribe(subscriber: Subscriber, channel: C): Unit =
    entries.computeIfPresent(channel, (_: C, subs: Set[Subscriber]) => {
      val newSubs = subs - subscriber
      if (newSubs.isEmpty) null
      else newSubs
    })

  def publish(event: E, channel: C): Unit =
    Option(entries get channel) foreach {
      _ foreach {
        publish(_, event)
      }
    }

  def size               = entries.size
  def sizeOf(channel: C) = Option(entries get channel).fold(0)(_.size)
}
