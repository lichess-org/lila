package lila.common

import scala.concurrent.duration._
import scala.concurrent.Promise

import akka.actor.{ ActorRef, ActorSystem }

object Bus {

  case class Event(payload: Any, channel: Symbol)
  type Classifier = Symbol
  type Subscriber = Tellable

  def publish(payload: Any, channel: Classifier): Unit = {
    publish(Bus.Event(payload, channel))
  }

  def subscribe = bus.subscribe _

  def subscribe(ref: ActorRef, to: Classifier) = bus.subscribe(Tellable(ref), to)

  def subscribe(subscriber: Tellable, to: Classifier*) = to foreach { bus.subscribe(subscriber, _) }
  def subscribe(ref: ActorRef, to: Classifier*) = to foreach { bus.subscribe(Tellable(ref), _) }
  def subscribe(ref: ActorRef, to: Iterable[Classifier]) = to foreach { bus.subscribe(Tellable(ref), _) }

  def subscribeFun(to: Classifier*)(f: PartialFunction[Any, Unit]): Tellable = {
    val t = lila.common.Tellable(f)
    subscribe(t, to: _*)
    t
  }

  def subscribeFuns(subscriptions: (Classifier, PartialFunction[Any, Unit])*): Unit =
    subscriptions foreach {
      case (classifier, subscriber) => subscribeFun(classifier)(subscriber)
    }

  def unsubscribe = bus.unsubscribe _
  def unsubscribe(ref: ActorRef, from: Classifier) = bus.unsubscribe(Tellable(ref), from)

  def unsubscribe(subscriber: Tellable, from: Iterable[Classifier]) = from foreach { bus.unsubscribe(subscriber, _) }
  def unsubscribe(ref: ActorRef, from: Iterable[Classifier]) = from foreach { bus.unsubscribe(Tellable(ref), _) }

  def publish(event: Event): Unit = bus.publish(event.payload, event.channel)

  def ask[A](classifier: Classifier, timeout: FiniteDuration = 1.second)(makeMsg: Promise[A] => Any)(
    implicit
    system: ActorSystem
  ): Fu[A] = {
    val promise = Promise[A]
    val msg = makeMsg(promise)
    publish(msg, classifier)
    promise.future.withTimeout(
      timeout,
      Bus.AskTimeout(s"Bus.ask timeout: $classifier $msg")
    )
  }

  private val bus = new EventBus[Any, Classifier, Tellable](
    initialCapacity = 65535,
    publish = (tellable, event) => tellable ! event
  )

  def size = bus.size

  case class AskTimeout(message: String) extends lila.base.LilaException

  private final class EventBus[Event, Channel, Subscriber](
      initialCapacity: Int,
      publish: (Subscriber, Event) => Unit
  ) {

    import java.util.concurrent.ConcurrentHashMap

    private val entries = new ConcurrentHashMap[Channel, Set[Subscriber]](initialCapacity)

    def subscribe(subscriber: Subscriber, channel: Channel): Unit =
      entries.compute(channel, (c: Channel, subs: Set[Subscriber]) => {
        Option(subs).fold(Set(subscriber))(_ + subscriber)
      })

    def unsubscribe(subscriber: Subscriber, channel: Channel): Unit =
      entries.computeIfPresent(channel, (c: Channel, subs: Set[Subscriber]) => {
        val newSubs = subs - subscriber
        if (newSubs.isEmpty) null
        else newSubs
      })

    def publish(event: Event, channel: Channel): Unit =
      Option(entries get channel) foreach {
        _ foreach {
          publish(_, event)
        }
      }

    def size = entries.size
    def sizeOf(channel: Channel) = Option(entries get channel).fold(0)(_.size)
  }
}
