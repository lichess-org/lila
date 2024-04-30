package lila.core
package bus

trait Tellable extends Any:
  def !(msg: Matchable): Unit
object Tellable:
  case class Actor(ref: akka.actor.ActorRef) extends Tellable:
    def !(msg: Matchable) = ref ! msg
  case class SyncActor(ref: scalalib.actor.SyncActor) extends Tellable:
    def !(msg: Matchable) = ref ! msg
  def apply(f: PartialFunction[Matchable, Unit]) = new Tellable:
    def !(msg: Matchable) = f.applyOrElse(msg, _ => ())

type Channel    = String
type Subscriber = Tellable
type Payload    = Matchable

trait WithChannel[T]:
  def channel: Channel

case class InChannel[T](channel: Channel) extends WithChannel[T]
