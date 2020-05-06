package lila.common

trait Tellable extends Any {
  def !(msg: Any): Unit
}

object Tellable {

  case class Actor(ref: akka.actor.ActorRef) extends Tellable {
    def !(msg: Any) = ref ! msg
  }

  def apply(f: PartialFunction[Any, Unit]) =
    new Tellable {
      def !(msg: Any) = f.applyOrElse(msg, doNothing)
    }

  private val doNothing = (_: Any) => ()
}
