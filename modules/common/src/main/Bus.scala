package lila.common

import scala.concurrent.duration._
import scala.concurrent.Promise
import scala.jdk.CollectionConverters._

import akka.actor.{ ActorRef, ActorSystem }

object Bus {

  case class Event(payload: Any, channel: String)
  type Channel    = String
  type Subscriber = Tellable

  def publish(payload: Any, channel: Channel): Unit = bus.publish(payload, channel)

  def subscribe = bus.subscribe _

  def subscribe(ref: ActorRef, to: Channel) = bus.subscribe(Tellable.Actor(ref), to)

  def subscribe(subscriber: Tellable, to: Channel*)   = to foreach { bus.subscribe(subscriber, _) }
  def subscribe(ref: ActorRef, to: Channel*)          = to foreach { bus.subscribe(Tellable.Actor(ref), _) }
  def subscribe(ref: ActorRef, to: Iterable[Channel]) = to foreach { bus.subscribe(Tellable.Actor(ref), _) }

  def subscribeFun(to: Channel*)(f: PartialFunction[Any, Unit]): Tellable = {
    val t = lila.common.Tellable(f)
    subscribe(t, to: _*)
    t
  }

  def subscribeFuns(subscriptions: (Channel, PartialFunction[Any, Unit])*): Unit =
    subscriptions foreach { case (channel, subscriber) =>
      subscribeFun(channel)(subscriber)
    }

  def unsubscribe                               = bus.unsubscribe _
  def unsubscribe(ref: ActorRef, from: Channel) = bus.unsubscribe(Tellable.Actor(ref), from)

  def unsubscribe(subscriber: Tellable, from: Iterable[Channel]) =
    from foreach {
      bus.unsubscribe(subscriber, _)
    }
  def unsubscribe(ref: ActorRef, from: Iterable[Channel]) =
    from foreach {
      bus.unsubscribe(Tellable.Actor(ref), _)
    }

  def ask[A](channel: Channel, timeout: FiniteDuration = 2.second)(makeMsg: Promise[A] => Any)(implicit
      ec: scala.concurrent.ExecutionContext,
      system: ActorSystem
  ): Fu[A] = {
    val promise = Promise[A]()
    val msg     = makeMsg(promise)
    publish(msg, channel)
    promise.future
      .withTimeout(
        timeout,
        Bus.AskTimeout(s"Bus.ask timeout: $channel $msg")
      )
      .monSuccess(_.bus.ask(s"${channel}_${msg.getClass}"))
  }

  private val bus = new EventBus[Any, Channel, Tellable](
    initialCapacity = 4096,
    publish = (tellable, event) => tellable ! event
  )

  def size = bus.size

  case class AskTimeout(message: String) extends lila.base.LilaException
}

final private class EventBus[Event, Channel, Subscriber](
    initialCapacity: Int,
    publish: (Subscriber, Event) => Unit
) {

  import java.util.concurrent.ConcurrentHashMap

  private val entries = new ConcurrentHashMap[Channel, Set[Subscriber]](initialCapacity)

  def subscribe(subscriber: Subscriber, channel: Channel): Unit =
    entries
      .compute(
        channel,
        (_: Channel, subs: Set[Subscriber]) => {
          Option(subs).fold(Set(subscriber))(_ + subscriber)
        }
      )
      .unit

  def unsubscribe(subscriber: Subscriber, channel: Channel): Unit =
    entries
      .computeIfPresent(
        channel,
        (_: Channel, subs: Set[Subscriber]) => {
          val newSubs = subs - subscriber
          if (newSubs.isEmpty) null
          else newSubs
        }
      )
      .unit

  def publish(event: Event, channel: Channel): Unit =
    Option(entries get channel) foreach {
      _ foreach {
        publish(_, event)
      }
    }

  def size = entries.size
}
