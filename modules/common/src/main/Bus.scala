package lila.common

import akka.actor._
import akka.event._

final class Bus(system: ActorSystem) extends Extension with EventBus {

  type Event = Bus.Event
  type Classifier = Symbol
  type Subscriber = ActorRef

  def publish(payload: Any, channel: Classifier) {
    publish(Bus.Event(payload, channel))
  }

  /**
   * Attempts to register the subscriber to the specified Classifier
   * @return true if successful and false if not (because it was already subscribed to that Classifier, or otherwise)
   */
  def subscribe(subscriber: Subscriber, to: Classifier): Boolean = {
    // println(s"subscribe $to $subscriber")
    bus.subscribe(subscriber, to)
  }

  def subscribe(subscriber: Subscriber, to: Classifier*): Boolean = {
    to foreach { subscribe(subscriber, _) }
    true
  }

  /**
   * Attempts to deregister the subscriber from the specified Classifier
   * @return true if successful and false if not (because it wasn't subscribed to that Classifier, or otherwise)
   */
  def unsubscribe(subscriber: Subscriber, from: Classifier): Boolean = {
    // println(s"[UN]subscribe $from $subscriber")
    bus.unsubscribe(subscriber, from)
  }

  /**
   * Attempts to deregister the subscriber from all Classifiers it may be subscribed to
   */
  def unsubscribe(subscriber: Subscriber) {
    // println(s"[UN]subscribe ALL $subscriber")
    bus unsubscribe subscriber
  }

  /**
   * Publishes the specified Event to this bus
   */
  def publish(event: Event) {
    // println(event)
    bus publish event
  }

  private val bus = new ActorEventBus with LookupClassification {

    type Event = Bus.Event
    type Classifier = Symbol

    override def mapSize() = 16

    def classify(event: Event): Symbol = event.channel

    def publish(event: Event, subscriber: ActorRef) = {
      subscriber ! event.payload
    }
  }
}

object Bus extends ExtensionId[Bus] with ExtensionIdProvider {

  case class Event(payload: Any, channel: Symbol)

  override def lookup = Bus

  override def createExtension(system: ExtendedActorSystem) = new Bus(system)
}

