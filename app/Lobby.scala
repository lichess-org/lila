package lila.http

import akka.actor._
import akka.util.duration._

import play.api._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import akka.util.Timeout
import akka.pattern.ask

import play.api.Play.current

object Lobby {

  implicit val timeout = Timeout(1 second)

  val instance = Akka.system.actorOf(Props[Lobby])

  def join(uid: String): Promise[(Iteratee[JsValue, _], Enumerator[JsValue])] = {
    (instance ? Join(uid)).asPromise.map {

      case Connected(enumerator) ⇒
        // Create an Iteratee to consume the feed
        val iteratee = Iteratee.foreach[JsValue] { event ⇒
          (event \ "t").as[String] match {
            case "talk" ⇒ instance ! Talk(
              (event \ "data" \ "txt").as[String],
              (event \ "data" \ "u").as[String]
            )
          }
        }.mapDone { _ ⇒ instance ! Quit(uid) }
        (iteratee, enumerator)

      case CannotConnect(error) ⇒
        // A finished Iteratee sending EOF
        val iteratee = Done[JsValue, Unit]((), Input.EOF)
        // Send an error and close the socket
        val enumerator = Enumerator[JsValue](JsObject(Seq("error" -> JsString(error)))).andThen(Enumerator.enumInput(Input.EOF))
        (iteratee, enumerator)
    }
  }
}

class Lobby extends Actor {

  var members = Map.empty[String, PushEnumerator[JsValue]]

  def receive = {

    case Join(uid) ⇒ {
      // Create an Enumerator to write to this socket
      val channel = Enumerator.imperative[JsValue]()
      members = members + (uid -> channel)
      sender ! Connected(channel)
    }

    case Talk(txt, u) ⇒ notifyAll("talk", Seq(
      "txt" -> JsString(txt),
      "u" -> JsString(u)
    ))

    case Quit(uid) ⇒ { members = members - uid }
  }

  def notifyAll(t: String, data: Seq[(String, JsValue)]) {
    val msg = JsObject(
      Seq(
        "t" -> JsString(t),
        "d" -> JsObject(data)
      )
    )
    members.foreach {
      case (_, channel) ⇒ channel.push(msg)
    }
  }

}

case class Join(uid: String)
case class Quit(uid: String)
case class Talk(txt: String, u: String)

case class Connected(enumerator: Enumerator[JsValue])
case class CannotConnect(msg: String)
