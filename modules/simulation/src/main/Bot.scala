package lila.simulation

import scala.concurrent.duration._

import akka.actor._
import ornicar.scalalib.Random
import play.api.libs.iteratee._
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json._

import lila.common.PimpedJson._
import lila.user.User

private[simulation] trait Bot extends SimulActor {

  import Bot._

  val uid = Random nextStringUppercase 8
  val user = none[User]
  val sid = none[String]
  val ip = "127.0.0.1"

  def getVersion(obj: JsObject): Option[Int] = obj int "v"

  def setVersion(obj: JsObject) {
    getVersion(obj) map SetVersion.apply foreach self.!
  }

  def receiveFrom(enumerator: Enumerator[JsValue]) {
    enumerator &> parsingMessage |>> Iteratee.foreach(self.!)
  }

  def sendTo(iteratee: Iteratee[JsValue, _]): JsChannel = {
    val (enumerator, channel) = Concurrent.broadcast[JsValue]
    enumerator |>> iteratee
    channel
  }

  def maybe(factor: Double)(action: => Unit): Boolean =
    if (scala.util.Random.nextDouble < factor) {
      action
      true
    }
    else false
}

private[simulation] object Bot {

  case object Ping
  case class SetVersion(v: Int)

  // type, full object
  case class Message(t: String, obj: JsObject)

  val parsingMessage: Enumeratee[JsValue, Message] =
    Enumeratee.mapInput[JsValue] {
      case Input.El(js) => parseMessage(js).fold[Input[Message]](Input.Empty)(Input.El.apply)
      case _            => Input.Empty
    }

  def parseMessage(js: JsValue): Option[Message] =
    js.asOpt[JsObject] flatMap { obj =>
      obj str "t" map { Message(_, obj) }
    }
}
