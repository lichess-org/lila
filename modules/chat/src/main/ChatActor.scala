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
    relationApi: lila.relation.RelationApi,
    prefApi: PrefApi) extends Actor {

  private val members = collection.mutable.Map[String, ChatMember[_]]()

  private val lichessBot = context.actorOf(Props[LichessBot])

  override def preStart() {
    bus.subscribe(self, 'chat, 'socketDoor, 'relation)
    val bot = new BotChatMember("lichess", lichessBot)
    members += (bot.uid -> bot)
  }

  override def postStop() {
    bus.unsubscribe(self)
  }

  def receive = {

    case line: Line ⇒ {
      lazy val json = Socket.makeMessage("chat.line", line.toJson)
      members.values foreach {
        case m: JsChatMember if (m wants line)  ⇒ m tell json
        case m: BotChatMember if (m wants line) ⇒ m tell line
        case _                                  ⇒
      }
    }

    case System(chanTyp: String, chanId: Option[String], text: String) ⇒
      Chan(chanTyp, chanId) foreach { chan ⇒
        api.systemWrite(chan, text) pipeTo self
      }

    case lila.hub.actorApi.relation.Block(u1, u2) ⇒ withMembersOf(u1) { member ⇒
      member block u2
      reloadChat(member)
    }

    case lila.hub.actorApi.relation.UnBlock(u1, u2) ⇒ withMembersOf(u1) { member ⇒
      member unBlock u2
      reloadChat(member)
    }

    case Input(uid, o) ⇒ (o str "t") |@| (o obj "d") |@| (members get uid) apply {
      case (typ, data, member) ⇒ typ match {

        case "chat.register" ⇒ relationApi blocking member.userId foreach { blocks ⇒
          member.setHead(ChatHead(
            chans = (~data.arrAs("chans")(_.asOpt[String]) map Chan.parse).flatten,
            pageChanKey = data str "pageChan",
            activeChanKeys = (~data.arrAs("activeChans")(_.asOpt[String])).toSet,
            mainChanKey = data str "mainChan"))
          member setBlocks blocks
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
        } {
          if (text startsWith "/") api.makeLine(chan, member.userId, text) foreach {
            _ foreach { line ⇒
              self ! line.copy(to = "lichess".some, text = text drop 1)
            }
          }
          else api.write(chan, member.userId, text) foreach { _ foreach self.! }

        }
      }
    }

    case SocketEnter(uid, member) ⇒ member.userId foreach { userId ⇒
      members += (uid -> new JsChatMember(uid, userId, member.troll, member.channel))
    }

    case SocketLeave(uid) ⇒ members -= uid
  }

  private def withMembersOf(userId: String)(f: ChatMember[_] ⇒ Unit) {
    members.values foreach { member ⇒
      if (member.userId == userId) f(member)
    }
  }

  private def withChan(data: JsObject)(f: Chan ⇒ Unit) {
    data str "chan" flatMap Chan.parse foreach f
  }

  private def withChanOption(data: JsObject)(f: Option[Chan] ⇒ Unit) {
    f(data str "chan" flatMap Chan.parse)
  }

  private def reloadChat(member: ChatMember[_]) {
    member match {
      case m: JsChatMember ⇒
        UserRepo byId m.userId flatten s"User of $m not found" foreach { user ⇒
          api.populate(m.head, user) foreach { chat ⇒
            m tell Socket.makeMessage("chat.reload", chat.toJson)
          }
        }
      case _ ⇒
    }
  }
}
