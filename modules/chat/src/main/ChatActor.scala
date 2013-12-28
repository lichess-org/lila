package lila.chat

import akka.actor._

import lila.common.Bus
import lila.common.PimpedJson._
import lila.hub.actorApi.chat._
import lila.user.UserRepo

private[chat] final class ChatActor(
    api: Api,
    bus: Bus,
    prefApi: lila.pref.PrefApi) extends Actor {

  bus.subscribe(self, 'chat)

  override def postStop() {
    bus.unsubscribe(self)
  }

  def receive = {

    case Input(userId, o) ⇒ (o str "t") |@| (o obj "d") apply {
      case (typ, data) ⇒ typ match {
        case "chat.set-active-chan" ⇒ data str "chan" foreach { chan ⇒
          val value = data str "value" exists ("true"==)
          UserRepo byId userId foreach {
            _ foreach { u ⇒ prefApi.setPref(u, _.updateChat(_.withChan(chan, value))) }
          }
        }
        case "chat.set-main" ⇒ UserRepo byId userId foreach {
          _ foreach { u ⇒ prefApi.setPref(u, _.updateChat(_.withMainChan(data str "chan"))) }
        }
        case "chat.tell" ⇒ for {
          chan ← data str "chan"
          text ← data str "text"
        } api.write(chan, userId, text) foreach {
          _ foreach { line ⇒
            val msg = AddLine(line.chan.key, line.troll, line.toJson)
            bus.publish(msg, 'socket)
          }
        }
      }
    }

  }
}
