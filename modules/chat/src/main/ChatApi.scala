package lila.chat

import chess.Color

import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class ChatApi(
    coll: Coll,
    chatTimeout: ChatTimeout,
    flood: lila.security.Flood,
    shutup: akka.actor.ActorSelection,
    modLog: akka.actor.ActorSelection,
    lilaBus: lila.common.Bus,
    maxLinesPerChat: Int,
    netDomain: String) {

  import Chat.userChatBSONHandler

  object userChat {

    def findOption(chatId: ChatId): Fu[Option[UserChat]] =
      coll.byId[UserChat](chatId)

    def find(chatId: ChatId): Fu[UserChat] =
      findOption(chatId) map (_ | Chat.makeUser(chatId))

    def findMine(chatId: ChatId, me: User): Fu[UserChat.Mine] = find(chatId) flatMap { chat =>
      (!chat.isEmpty ?? chatTimeout.isActive(chatId, me.id)) map {
        UserChat.Mine(chat forUser me.some, _)
      }
    }

    def findMine(chatId: ChatId, me: Option[User]): Fu[UserChat.Mine] = me match {
      case Some(user) => findMine(chatId, user)
      case None       => find(chatId) map { UserChat.Mine(_, false) }
    }

    def write(chatId: ChatId, userId: String, text: String, public: Boolean): Funit =
      makeLine(chatId, userId, text) flatMap {
        _ ?? { line =>
          pushLine(chatId, line) >>- {
            shutup ! {
              import lila.hub.actorApi.shutup._
              if (public) RecordPublicChat(chatId, userId, text)
              else RecordPrivateChat(chatId, userId, text)
            }
            lilaBus.publish(actorApi.ChatLine(chatId, line), channelOf(chatId))
          }
        }
      }

    def system(chatId: ChatId, text: String) = {
      val line = UserLine(systemUserId, Writer delocalize text, troll = false, deleted = false)
      pushLine(chatId, line) inject line.some
    }

    def timeout(chatId: ChatId, modId: String, userId: String, reason: ChatTimeout.Reason): Funit =
      coll.byId[UserChat](chatId) zip UserRepo.byId(modId) zip UserRepo.byId(userId) flatMap {
        case ((Some(chat), Some(mod)), Some(user)) if isMod(mod) => doTimeout(chat, mod, user, reason)
        case _ => fuccess(none)
      }

    def userModInfo(username: String): Fu[Option[UserModInfo]] =
      UserRepo named username flatMap {
        _ ?? { user =>
          chatTimeout.history(user, 20) map { UserModInfo(user, _).some }
        }
      }

    private def doTimeout(c: UserChat, mod: User, user: User, reason: ChatTimeout.Reason): Funit = {
      val line = UserLine(
        username = systemUserId,
        text = s"${user.username} was timed out 10 minutes for ${reason.name}.",
        troll = false, deleted = false)
      val chat = c.markDeleted(user) add line
      coll.update($id(chat.id), chat).void >>
        chatTimeout.add(c, mod, user, reason) >>- {
          lilaBus.publish(actorApi.OnTimeout(user.username), channelOf(chat.id))
          lilaBus.publish(actorApi.ChatLine(chat.id, line), channelOf(chat.id))
          modLog ! lila.hub.actorApi.mod.ChatTimeout(
            mod = mod.id, user = user.id, reason = reason.key)
        }
    }

    def reinstate(list: List[ChatTimeout.Reinstate]) = list.foreach { r =>
      lilaBus.publish(actorApi.OnReinstate(r.user), Symbol(s"chat-${r.chat}"))
    }

    private def isMod(user: User) = lila.security.Granter(_.MarkTroll)(user)

    private[ChatApi] def makeLine(chatId: String, userId: String, t1: String): Fu[Option[UserLine]] =
      UserRepo.byId(userId) zip chatTimeout.isActive(chatId, userId) map {
        case (Some(user), false) if !user.disabled => Writer cut t1 flatMap { t2 =>
          flood.allowMessage(user.id, t2) option
            UserLine(user.username, Writer preprocessUserInput t2, troll = user.troll, deleted = false)
        }
        case _ => none
      }
  }

  object playerChat {

    def findOption(chatId: ChatId): Fu[Option[MixedChat]] =
      coll.byId[MixedChat](chatId)

    def find(chatId: ChatId): Fu[MixedChat] =
      findOption(chatId) map (_ | Chat.makeMixed(chatId))

    def findNonEmpty(chatId: ChatId): Fu[Option[MixedChat]] =
      findOption(chatId) map (_ filter (_.nonEmpty))

    def write(chatId: ChatId, color: Color, text: String): Funit =
      makeLine(chatId, color, text) ?? { line =>
        pushLine(chatId, line) >>-
          lilaBus.publish(actorApi.ChatLine(chatId, line), channelOf(chatId))
      }

    private def makeLine(chatId: ChatId, color: Color, t1: String): Option[Line] =
      Writer cut t1 flatMap { t2 =>
        flood.allowMessage(s"$chatId/${color.letter}", t2) option
          PlayerLine(color, Writer preprocessUserInput t2)
      }
  }

  private def pushLine(chatId: ChatId, line: Line): Funit = coll.update(
    $id(chatId),
    $doc("$push" -> $doc(
      Chat.BSONFields.lines -> $doc(
        "$each" -> List(Line.lineBSONHandler(false).write(line)),
        "$slice" -> -maxLinesPerChat)
    )),
    upsert = true
  ).void >>- lila.mon.chat.message()

  private def channelOf(id: String) = Symbol(s"chat-$id")

  private object Writer {

    import java.util.regex.Matcher.quoteReplacement

    def preprocessUserInput(in: String) = delocalize(noPrivateUrl(in))

    def cut(text: String) = Some(text.trim take 140) filter (_.nonEmpty)
    val delocalize = new lila.common.String.Delocalizer(netDomain)
    val domainRegex = netDomain.replace(".", """\.""")
    val gameUrlRegex = (domainRegex + """\b/([\w]{8})[\w]{4}\b""").r
    def noPrivateUrl(str: String): String =
      gameUrlRegex.replaceAllIn(str, m => quoteReplacement(netDomain + "/" + (m group 1)))
  }
}
