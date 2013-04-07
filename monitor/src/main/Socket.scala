package lila.monitor

import akka.actor._
import akka.pattern.ask
import scala.concurrent.duration._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits._

import lila.socket.{ Ping, Quit, WithSocket }
import lila.common.PimpedJson._

private[monitor] final class Socket(hub: ActorRef) with WithSocket {

  implicit val timeout = makeTimeout(300 millis)

  // TODO
  // def join(uid: String): SocketFuture = {
  //   val promise: Option[SocketFuture] = (hub ? Join(uid)) map {
  //     case Connected(enumerator, channel) ⇒
  //       val iteratee = Iteratee.foreach[JsValue] { e ⇒
  //         e str "t" match {
  //           case Some("p") ⇒ hub ! Ping(uid)
  //           case _         ⇒
  //         }
  //       } mapDone { _ ⇒
  //         hub ! Quit(uid)
  //       }
  //       (iteratee, enumerator)
  //   }
  //   promise | connectionFail
}
