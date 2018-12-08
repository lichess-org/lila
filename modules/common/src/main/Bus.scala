package lila.common

import scala.concurrent.duration._
import scala.concurrent.Promise

import akka.actor._
import akka.event._

// can only ever be instanciated once per actor system
final class Bus private (system: ActorSystem) extends Extension with EventBus {

  type Event = Bus.Event
  type Classifier = Symbol
  type Subscriber = Tellable

  def publish(payload: Any, channel: Classifier): Unit = {
    publish(Bus.Event(payload, channel))
  }

  def subscribe(subscriber: Tellable, to: Classifier): Boolean = {
    bus.subscribe(subscriber, to)
  }
  def subscribe(ref: ActorRef, to: Classifier): Boolean = subscribe(Tellable(ref), to)

  def subscribe(subscriber: Tellable, to: Classifier*): Boolean = {
    to foreach { subscribe(subscriber, _) }
    true
  }
  def subscribe(ref: ActorRef, to: Classifier*): Boolean = subscribe(Tellable(ref), to: _*)

  def subscribeFun(to: Classifier*)(f: PartialFunction[Any, Unit]): Tellable = {
    val t = lila.common.Tellable(f)
    subscribe(t, to: _*)
    t
  }

  def subscribeFuns(subscriptions: (Classifier, PartialFunction[Any, Unit])*): Unit =
    subscriptions foreach {
      case (classifier, subscriber) => subscribeFun(classifier)(subscriber)
    }

  def unsubscribe(subscriber: Tellable, from: Classifier): Boolean = bus.unsubscribe(subscriber, from)
  def unsubscribe(ref: ActorRef, from: Classifier): Boolean = unsubscribe(Tellable(ref), from)

  def unsubscribe(subscriber: Tellable, from: Seq[Classifier]): Boolean =
    from forall { unsubscribe(subscriber, _) }
  def unsubscribe(ref: ActorRef, from: Seq[Classifier]): Boolean =
    unsubscribe(Tellable(ref), from)

  def unsubscribe(subscriber: Tellable): Unit = bus unsubscribe subscriber
  def unsubscribe(ref: ActorRef): Unit = unsubscribe(Tellable(ref))

  def publish(event: Event): Unit = bus publish event

  def ask[A](classifier: Classifier, timeout: FiniteDuration = 1.second)(makeMsg: Promise[A] => Any): Fu[A] = {
    val promise = Promise[A]
    val msg = makeMsg(promise)
    publish(msg, classifier)
    promise.future.withTimeout(
      timeout,
      Bus.AskTimeout(s"Bus.ask timeout: $classifier $msg")
    )(system)
  }

  private val bus = new EventBus with LookupClassification {

    type Event = Bus.Event
    type Classifier = Symbol
    type Subscriber = Tellable

    override protected val mapSize = 65536

    protected def compareSubscribers(a: Tellable, b: Tellable) = a.uniqueId compareTo b.uniqueId

    def classify(event: Event): Symbol = event.channel

    def publish(event: Event, subscriber: Tellable) =
      subscriber ! event.payload

    import scala.concurrent.duration._
    system.scheduler.schedule(1 minute, 1 minute) {
      lila.mon.bus.classifiers(subscribers.keys.size)
      lila.mon.bus.subscribers(subscribers.values.size)
    }
  }
}

object Bus extends ExtensionId[Bus] with ExtensionIdProvider {

  case class Event(payload: Any, channel: Symbol)

  override def lookup() = Bus

  override def createExtension(system: ExtendedActorSystem) = new Bus(system)

  case class AskTimeout(message: String) extends lila.base.LilaException
}
