package lila
package game

import socket.{ History, LilaEnumerator }
import model._
import socket._
import chess.Color

import akka.actor._
import akka.event.Logging
import play.api.libs.json._
import play.api.libs.iteratee._

final class Hub(gameId: String, history: History) extends Actor {

  private var members = Map.empty[String, Member]
  private val log = Logging(context.system, this)

  def receive = {

    case WithMembers(op) ⇒ op(members.values.pp).unsafePerformIO

    case GetVersion      ⇒ sender ! Version(history.version)

    case GetNbMembers    ⇒ sender ! members.size

    case NbPlayers(nb)   ⇒ notifyAll("nbp", JsNumber(nb))

    case Join(uid, version, color, owner, username) ⇒ {
      val channel = new LilaEnumerator[JsValue](history since version)
      val member = Member(channel, PovRef(gameId, color), owner, username)
      members = members + (uid -> member)
      sender ! Connected(member)
    }

    case Events(events) ⇒ events foreach notifyVersion

    case Quit(uid)      ⇒ { members = members - uid }

    case Close ⇒ {
      members.values foreach { _.channel.close() }
      self ! PoisonPill
    }

    case msg ⇒ log.info("GameHub unknown message: " + msg)
  }

  private def notifyVersion(e: Event) {
    val vmsg = history += makeMessage(e.typ, e.data)
    val m1 = if (e.owner) members.values filter (_.owner) else members.values
    val m2 = e.only.fold(color ⇒ m1 filter (_.color == color), m1)

    m2 foreach (_.channel push vmsg)
  }

  private def notifyAll(t: String, data: JsValue) {
    val msg = makeMessage(t, data)
    members.values.foreach(_.channel push msg)
  }

  private def makeMessage(t: String, data: JsValue) =
    JsObject(Seq("t" -> JsString(t), "d" -> data))
}
