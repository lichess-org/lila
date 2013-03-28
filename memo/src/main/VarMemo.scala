package lila.memo

import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._
import scala.reflect._
import akka.actor._
import akka.pattern.ask

import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.concurrent.Akka
import play.api.Play.current

final class VarMemo[A : ClassTag](load: Fu[A], atMost: FiniteDuration = 10.seconds) {

  private case object Get
  private implicit val timeout = makeTimeout(atMost)

  def get: Fu[A] = (actor ? Get).mapTo[A]

  def reload(fa: Fu[A]): Funit = fuccess(actor ! fa)

  private val actor = Akka.system.actorOf(Props(new Actor {

    private var value: Option[A] = none
    private def reset(a: A) { value = a.some }

    def receive = {

      case Get ⇒ value match {
        case None ⇒ sender ! (load.await ~ reset)
        case Some(v) ⇒ sender ! v
      }

      case fa: Fu[A] ⇒ reset(fa.await)
    }
  }))
}
