package lila.common

trait Tellable extends Any {
  def !(msg: Any): Unit
  def uniqueId: String
}

object Tellable {

  trait HashCode extends Tellable {
    lazy val uniqueId = Integer.toHexString(hashCode)
  }

  case class Actor(ref: akka.actor.ActorRef) extends AnyVal with Tellable {
    def !(msg: Any) = ref ! msg
    def uniqueId = ref.path.name
  }

  def apply(ref: akka.actor.ActorRef) = Actor(ref)
}
