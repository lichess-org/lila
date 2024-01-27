package lila.common

import scala.jdk.CollectionConverters.*

import akka.actor.{ ActorRef, Scheduler }

object Bus:

  type Channel    = String
  type Subscriber = Tellable

  case class Event(payload: Matchable, channel: Channel)

  def publish(payload: Matchable, channel: Channel): Unit = bus.publish(payload, channel)

  export bus.{ size, subscribe, unsubscribe }

  def subscribe(ref: ActorRef, to: Channel) = bus.subscribe(Tellable.Actor(ref), to)

  def subscribe(subscriber: Tellable, to: Channel*)   = to foreach { bus.subscribe(subscriber, _) }
  def subscribe(ref: ActorRef, to: Channel*)          = to foreach { bus.subscribe(Tellable.Actor(ref), _) }
  def subscribe(ref: ActorRef, to: Iterable[Channel]) = to foreach { bus.subscribe(Tellable.Actor(ref), _) }

  def subscribeFun(to: Channel*)(f: PartialFunction[Matchable, Unit]): Tellable =
    val t = Tellable(f)
    subscribe(t, to*)
    t

  def subscribeFuns(subscriptions: (Channel, PartialFunction[Matchable, Unit])*): Unit =
    subscriptions.foreach: (channel, subscriber) =>
      subscribeFun(channel)(subscriber)

  def unsubscribe(ref: ActorRef, from: Channel) = bus.unsubscribe(Tellable.Actor(ref), from)

  def unsubscribe(subscriber: Tellable, from: Iterable[Channel]) =
    from.foreach:
      bus.unsubscribe(subscriber, _)
  def unsubscribe(ref: ActorRef, from: Iterable[Channel]) =
    from.foreach:
      bus.unsubscribe(Tellable.Actor(ref), _)

  def ask[A](channel: Channel, timeout: FiniteDuration = 2.second)(makeMsg: Promise[A] => Matchable)(using
      Executor,
      Scheduler
  ): Fu[A] =
    val promise = Promise[A]()
    val msg     = makeMsg(promise)
    publish(msg, channel)
    promise.future
      .withTimeout(timeout, s"Bus.ask $channel $msg")
      .monSuccess(_.bus.ask(s"${channel}_${msg.getClass}"))

  private val bus = EventBus[Matchable, Channel, Tellable](
    initialCapacity = 4096,
    publish = (tellable, event) => tellable ! event
  )

final private class EventBus[Event, Channel, Subscriber](
    initialCapacity: Int,
    publish: (Subscriber, Event) => Unit
):

  private val entries = java.util.concurrent.ConcurrentHashMap[Channel, Set[Subscriber]](initialCapacity)
  export entries.size

  def subscribe(subscriber: Subscriber, channel: Channel): Unit =
    entries
      .compute(
        channel,
        (_: Channel, subs: Set[Subscriber]) => Option(subs).fold(Set(subscriber))(_ + subscriber)
      )

  def unsubscribe(subscriber: Subscriber, channel: Channel): Unit =
    entries
      .computeIfPresent(
        channel,
        (_: Channel, subs: Set[Subscriber]) =>
          val newSubs = subs - subscriber
          if newSubs.isEmpty then null
          else newSubs
      )

  def publish(event: Event, channel: Channel): Unit =
    Option(entries get channel).foreach:
      _ foreach:
        publish(_, event)
