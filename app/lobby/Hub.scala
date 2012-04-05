package lila
package lobby

import akka.actor._

import play.api.libs.json._
import play.api.libs.iteratee._

final class Hub(env: SystemEnv) extends Actor {

  private var members = Map.empty[String, PushEnumerator[JsValue]]

  def receive = {

    case Join(uid) ⇒ {
      // Create an Enumerator to write to this socket
      val channel = Enumerator.imperative[JsValue]()
      members = members + (uid -> channel)
      sender ! Connected(channel)
    }

    case Talk(txt, u) ⇒ env.messageRepo.add(txt, u).foreach { save ⇒
      val message = save.unsafePerformIO
      notifyAll("talk", Seq(
        "txt" -> JsString(message.text),
        "u" -> JsString(message.username)
      ))
    }

    case Entry(entry) ⇒ notifyAll("entry", Seq(
      "html" -> JsString(entry.render)
    ))

    case Quit(uid) ⇒ { members = members - uid }
  }

  def notifyAll(t: String, data: Seq[(String, JsValue)]) {
    val msg = JsObject(Seq("t" -> JsString(t), "d" -> JsObject(data)))
    members.foreach { case (_, channel) ⇒ channel.push(msg) }
  }
}
