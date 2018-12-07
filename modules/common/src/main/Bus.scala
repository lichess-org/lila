package lila.common

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

  def subscribeFun(to: Classifier*)(f: PartialFunction[Any, Unit]): ActorRef = {
    val actor = system.actorOf(Props(new Actor { val receive = f }))
    subscribe(Tellable(actor), to: _*)
    actor
  }

  def unsubscribe(subscriber: Tellable, from: Classifier): Boolean = bus.unsubscribe(subscriber, from)
  def unsubscribe(ref: ActorRef, from: Classifier): Boolean = unsubscribe(Tellable(ref), from)

  def unsubscribe(subscriber: Tellable): Unit = bus unsubscribe subscriber
  def unsubscribe(ref: ActorRef): Unit = unsubscribe(Tellable(ref))

  def publish(event: Event): Unit = bus publish event

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
}
