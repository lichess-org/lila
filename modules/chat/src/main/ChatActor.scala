package lila.chat

import akka.actor._
import akka.pattern.pipe
import play.api.libs.json.JsObject

import actorApi._
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

  private val members = collection.mutable.Map[String, ChatMember]()

  private val commander = context.actorOf(Props[Commander])

  override def preStart() {
    bus.subscribe(self, 'chat, 'socketDoor, 'relation)
  }

  override def postStop() {
    bus.unsubscribe(self)
  }

  def receive = {

    case line: Line ⇒ {
      lazy val json = lineMessage(line)
      members.values foreach { m ⇒
        if (m wants line) m tell json
      }
    }

    case Tell(uid, line) ⇒ {
      members get uid foreach { _ tell lineMessage(line) }
    }

    case System(chanTyp, chanId, text) ⇒ Chan(chanTyp, chanId) foreach { chan ⇒
      api.systemWrite(chan, text) pipeTo self
    }

    case SetOpen(member, value) ⇒
      prefApi.setPref(member.userId, (p: Pref) ⇒ p.updateChat(_.copy(on = value)))

    case Query(member, toId) ⇒
      UserRepo byId toId flatten s"Can't query non existing user $toId" foreach { to ⇒
        relationApi.follows(to.id, member.userId) foreach {
          case false ⇒ fufail(s"Can't query $toId, not following ${member.userId}")
          case _ ⇒ {
            val chan = UserChan(member.userId, toId)
            prefApi.setPref(member.userId, (p: Pref) ⇒
              p.updateChat(_.withChan(chan.key, true).withActiveChan(chan.key, true).withMainChan(chan.key.some))
            ) >>- {
              member addChan chan
              member.setActiveChan(chan.key, true)
              member setMainChan chan.key.some
              reloadChat(member)
            }
          }
        }
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
            _.withActiveChan(chan.key, value).withMainChan(chan.key.some),
            _.withActiveChan(chan.key, value)
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
        case "chat.tell" ⇒ data str "text" foreach { text ⇒
          val chanOption = data str "chan" flatMap Chan.parse
          if (text startsWith "/") commander ! Command(chanOption, member, text drop 1)
          else chanOption foreach { chan ⇒
            api.write(chan.key, member.userId, text) foreach { _ foreach self.! }
          }
        }
      }
    }

    case SocketEnter(uid, member) ⇒ member.userId foreach { userId ⇒
      members += (uid -> new ChatMember(uid, userId, member.troll, member.channel))
    }

    case SocketLeave(uid) ⇒ members -= uid
  }

  private def lineMessage(line: Line) = Socket.makeMessage("chat.line", line.toJson)

  private def withMembersOf(userId: String)(f: ChatMember ⇒ Unit) {
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

  private def reloadChat(m: ChatMember) {
    UserRepo byId m.userId flatten s"User of $m not found" foreach { user ⇒
      api.populate(m.head, user) foreach { chat ⇒
        m tell Socket.makeMessage("chat.reload", chat.toJson)
      }
    }
  }
}
