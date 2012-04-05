package lila

import akka.actor._
import akka.util.duration._
import akka.util.Timeout
import akka.pattern.ask

import play.api._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import play.api.Play.current

import scalaz.effects._

object Lobby {

  implicit val timeout = Timeout(1 second)

  lazy val instance = Akka.system.actorOf(Props(new Lobby(
    env = Global.env
  )))

  def join(uid: String): Promise[(Iteratee[JsValue, _], Enumerator[JsValue])] =
    (instance ? Join(uid)).asPromise.map {
      case Connected(enumerator) ⇒
        val iteratee = Iteratee.foreach[JsValue] { event ⇒
          (event \ "t").as[String] match {
            case "talk" ⇒ instance ! Talk(
              (event \ "data" \ "txt").as[String],
              (event \ "data" \ "u").as[String]
            )
          }
        }.mapDone { _ ⇒ instance ! Quit(uid) }
        (iteratee, enumerator)
    }
}

final class Lobby private (env: SystemEnv) extends Actor {

  private var members = Map.empty[String, PushEnumerator[JsValue]]

  def receive = {

    case Join(uid) ⇒ {
      // Create an Enumerator to write to this socket
      val channel = Enumerator.imperative[JsValue]()
      members = members + (uid -> channel)
      sender ! Connected(channel)
    }

    case Talk(txt, u) ⇒ env.messageRepo.add(txt, u).foreach { save ⇒
      save flatMap { message ⇒
        notifyAll("talk", Seq(
          "txt" -> JsString(message.text),
          "u" -> JsString(message.username)
        ))
      } unsafePerformIO
    }

    case Quit(uid) ⇒ { members = members - uid }
  }

  def notifyAll(t: String, data: Seq[(String, JsValue)]): IO[Unit] = io {
    val msg = JsObject(Seq("t" -> JsString(t), "d" -> JsObject(data)))
    members.foreach { case (_, channel) ⇒ channel.push(msg) }
  }
}

case class Join(uid: String)
case class Quit(uid: String)
case class Talk(txt: String, u: String)
case class Connected(enumerator: Enumerator[JsValue])
