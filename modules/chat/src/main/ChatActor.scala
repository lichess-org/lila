package lila.chat

import akka.actor._
import akka.pattern.pipe
import play.api.libs.json.JsObject

import lila.common.Bus
import lila.common.PimpedJson._
import lila.hub.actorApi.chat._
import lila.pref.{ Pref, PrefApi }
import lila.socket.actorApi.{ SocketEnter, SocketLeave }
import lila.socket.Socket
import lila.user.UserRepo

private[chat] final class ChatActor(
    api: Api,
    namer: Namer,
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

        case "chat.register" ⇒ {
          member.setHead(ChatHead(
            chans = (~data.arrAs("chans")(_.asOpt[String]) map Chan.parse).flatten,
            pageChanKey = data str "pageChan",
            activeChanKeys = (~data.arrAs("activeChans")(_.asOpt[String])).toSet,
            mainChanKey = data str "mainChan"))
          reloadChat(member)
        }
        case "chat.set-active-chan" ⇒ withChan(data) { chan ⇒
          val value = data int "value" exists (1==)
          val setMain = value && (data int "main" exists (1==))
          (!chan.autoActive) ?? prefApi.setPref(member.userId, (p: Pref) ⇒ p.updateChat(setMain.fold(
            _.withChan(chan.key, value).withMainChan(chan.key.some),
            _.withChan(chan.key, value)
          ))) andThen {
            case _ ⇒ {
              member.setActiveChan(chan.key, value)
              if (setMain) member.setMainChan(chan.key.some)
              reloadChat(member)
            }
          }
        }
        case "chat.set-main" ⇒ withChanOption(data) { chan ⇒
          member.setMainChan(chan map (_.key))
          if (!chan.??(_.autoActive)) prefApi.setPref(member.userId, (p: Pref) ⇒
            p.updateChat(_.withMainChan(chan map (_.key)))
          )
        }
        case "chat.tell" ⇒ for {
          chan ← data str "chan"
          text ← data str "text"
        } api.write(chan, member.userId, text) foreach { _ foreach addLine }
      }
    }

    case SocketEnter(uid, member) ⇒ member.userId foreach { userId ⇒
      members += (uid -> new ChatMember(uid, userId, member.troll, member.channel))
    }

    case SocketLeave(uid) ⇒ members -= uid
  }

  def addLine(line: Line) {
    val msg = Socket.makeMessage("chat.line", line.toJson)
    members.values foreach { m ⇒
      if (m wants line) m.channel push msg
    }
  }

  private def withChan(data: JsObject)(f: Chan ⇒ Unit) {
    data str "chan" flatMap Chan.parse foreach f
  }

  private def withChanOption(data: JsObject)(f: Option[Chan] ⇒ Unit) {
    f(data str "chan" flatMap Chan.parse)
  }

  private def reloadChat(member: ChatMember) {
    UserRepo byId member.userId flatten s"User of $member not found" foreach { user ⇒
      api.populate(member.head, user) foreach { chat ⇒
        member.channel push Socket.makeMessage("chat.reload", chat.toJson)
      }
    }
  }
}
