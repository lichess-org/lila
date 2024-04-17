package lila.common

import akka.actor.{ ActorRef, Scheduler }

import scala.jdk.CollectionConverters.*

trait Tellable extends Any:
  def !(msg: Matchable): Unit
object Tellable:
  case class Actor(ref: akka.actor.ActorRef) extends Tellable:
    def !(msg: Matchable) = ref ! msg
  case class SyncActor(ref: scalalib.actor.SyncActor) extends Tellable:
    def !(msg: Matchable) = ref ! msg
  def apply(f: PartialFunction[Matchable, Unit]) = new Tellable:
    def !(msg: Matchable) = f.applyOrElse(msg, _ => ())

object NamedBus:
  object fishnet:
    import lila.core.fishnet.*
    def analyseGame(gameId: GameId): Unit                   = Bus.publish(GameRequest(gameId), "fishnet")
    def analyseStudyChapter(req: StudyChapterRequest): Unit = Bus.publish(req, "fishnet")
  object timeline:
    import lila.core.timeline.*
    def apply(propagate: Propagate): Unit = Bus.publish(propagate, "timeline")

final class BusChannel(channel: Bus.Channel):
  def apply(msg: Bus.Payload): Unit                       = Bus.publish(msg, channel)
  def subscribe(subscriber: Bus.SubscriberFunction): Unit = Bus.subscribeFun(channel)(subscriber)

object BusChannel:
  val forumPost = BusChannel("forumPost")

object Bus:

  type Channel    = String
  type Subscriber = Tellable
  type Payload    = Matchable

  type SubscriberFunction = PartialFunction[Payload, Unit]

  val named = NamedBus
  val chan  = BusChannel

  def publish(payload: Payload, channel: Channel): Unit = bus.publish(payload, channel)

  export bus.{ size, subscribe, unsubscribe }

  def subscribe(ref: ActorRef, to: Channel) = bus.subscribe(Tellable.Actor(ref), to)

  def subscribe(subscriber: Tellable, to: Channel*)   = to.foreach { bus.subscribe(subscriber, _) }
  def subscribe(ref: ActorRef, to: Channel*)          = to.foreach { bus.subscribe(Tellable.Actor(ref), _) }
  def subscribe(ref: ActorRef, to: Iterable[Channel]) = to.foreach { bus.subscribe(Tellable.Actor(ref), _) }
  def subscribe(ref: scalalib.actor.SyncActor, to: Channel*) = to.foreach {
    bus.subscribe(Tellable.SyncActor(ref), _)
  }

  def subscribeFun(to: Channel*)(f: SubscriberFunction): Tellable =
    val t = Tellable(f)
    subscribe(t, to*)
    t

  def subscribeFuns(subscriptions: (Channel, SubscriberFunction)*): Unit =
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

  private val bus = EventBus[Payload, Channel, Tellable](
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
    Option(entries.get(channel)).foreach:
      _.foreach:
        publish(_, event)
