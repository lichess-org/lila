package lila.chat

import akka.actor._
import akka.pattern.pipe

import lila.common.Bus
import lila.common.PimpedJson._
import lila.hub.actorApi.chat._
import lila.pref.{ Pref, PrefApi }
import lila.socket.actorApi.{ SocketEnter, SocketLeave }
import lila.socket.Socket
import lila.user.UserRepo

private[chat] final class ChatActor(
    api: Api,
    bus: Bus,
    prefApi: PrefApi) extends Actor {

  private val members = collection.mutable.Map[String, ChatMember]()

  bus.subscribe(self, 'chat, 'socketDoor)

  override def postStop() {
    bus.unsubscribe(self)
  }

  def receive = {

    case Input(uid, o) ⇒ (o str "t") |@| (o obj "d") |@| (members get uid) apply {
      case (typ, data, member) ⇒ typ match {
        case "chat.set-active-chan" ⇒ data str "chan" foreach { chan ⇒
          val value = data int "value" exists (1==)
          val setMain = value && (data int "main" exists (1==))
          prefApi.setPref(member.userId, (p: Pref) ⇒ p.updateChat(setMain.fold(
            _.withChan(chan, value).withMainChan(chan.some),
            _.withChan(chan, value)
          ))) andThen {
            case _ ⇒ {
              member setActiveChan(chan, value)
              reloadChat(member)
            }
          }
        }
        case "chat.set-main" ⇒
          prefApi.setPref(member.userId, (p: Pref) ⇒ p.updateChat(_.withMainChan(data str "chan")))
        case "chat.register" ⇒ prefApi getPref member.userId foreach { pref ⇒
          member setActiveChans pref.chat.chans
          member setExtraChans {
            data str "chans" map (_.split(',').toList) getOrElse Nil
          }
          reloadChat(member)
        }
        case "chat.tell" ⇒ for {
          chan ← data str "chan"
          text ← data str "text"
        } api.write(chan, member.userId, text) foreach { _ foreach addLine }
      }
    }

    case SocketEnter(uid, member, socket) ⇒ member.userId foreach { userId ⇒
      members += (uid -> ChatMember(uid, userId, member.troll, member.channel))
    }

    case SocketLeave(uid) ⇒ members -= uid
  }

  def addLine(line: Line) {
    val msg = Socket.makeMessage("chat.line", line.toJson)
    members.values foreach { m ⇒
      if (m wants line) m.channel push msg
    }
  }

  private def reloadChat(member: ChatMember) {
    api.get(member.userId, member.extraChans) foreach {
      case chat: Chat ⇒ member.channel push Socket.makeMessage("chat.reload", chat.toJson)
    }
  }
}
