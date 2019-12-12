package lila.common

trait Tellable extends Any {
  def !(msg: Any): Unit
}

object Tellable {

  def apply(f: PartialFunction[Any, Unit]) = new Tellable {
    def !(msg: Any) = f.applyOrElse(msg, doNothing)
  }
  def apply(ref: akka.actor.ActorRef) = new Tellable {
    def !(msg: Any) = ref ! msg
  }

  private val doNothing = (_: Any) => ()
}
