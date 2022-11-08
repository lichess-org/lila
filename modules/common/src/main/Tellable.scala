package lila.common

trait Tellable extends Any:
  def !(msg: Matchable): Unit

object Tellable:

  case class Actor(ref: akka.actor.ActorRef) extends Tellable:
    def !(msg: Matchable) = ref ! msg

  def apply(f: PartialFunction[Matchable, Unit]) =
    new Tellable:
      def !(msg: Matchable) = f.applyOrElse(msg, doNothing)

  private val doNothing = (_: Matchable) => ()
