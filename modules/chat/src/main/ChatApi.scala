package lila.chat

import chess.Color
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

import lila.db.dsl._
import lila.hub.actorApi.shutup.{ PublicSource, RecordPublicChat, RecordPrivateChat }
import lila.user.{ User, UserRepo }

final class ChatApi(
    coll: Coll,
    chatTimeout: ChatTimeout,
    flood: lila.security.Flood,
    shutup: akka.actor.ActorSelection,
    modLog: akka.actor.ActorSelection,
    asyncCache: lila.memo.AsyncCache.Builder,
    lilaBus: lila.common.Bus,
    maxLinesPerChat: Int,
    netDomain: String
) {

  import Chat.userChatBSONHandler
  import Chat.chatIdBSONHandler

  object userChat {

    // only use for public, multi-user chats - tournaments, simuls
    object cached {

      private val cache = asyncCache.clearable[Chat.Id, UserChat](
        name = "chat.user",
        f = find,
        expireAfter = _.ExpireAfterAccess(1 minute)
      )

      def invalidate = cache.invalidate _

      def findMine(chatId: Chat.Id, me: Option[User]): Fu[UserChat.Mine] = me match {
        case Some(user) => findMine(chatId, user)
        case None => cache.get(chatId) dmap { UserChat.Mine(_, false) }
      }

      private def findMine(chatId: Chat.Id, me: User): Fu[UserChat.Mine] = cache.get(chatId) flatMap { chat =>
        (!chat.isEmpty ?? chatTimeout.isActive(chatId, me.id)) dmap {
          UserChat.Mine(chat forUser me.some, _)
        }
      }
    }

    def findOption(chatId: Chat.Id): Fu[Option[UserChat]] =
      coll.byId[UserChat](chatId.value)

    def find(chatId: Chat.Id): Fu[UserChat] =
      findOption(chatId) dmap (_ | Chat.makeUser(chatId))

    def findAll(chatIds: List[Chat.Id]): Fu[List[UserChat]] =
      coll.byIds[UserChat](chatIds.map(_.value), ReadPreference.secondaryPreferred)

    def findMine(chatId: Chat.Id, me: Option[User]): Fu[UserChat.Mine] = findMineIf(chatId, me, true)

    def findMineIf(chatId: Chat.Id, me: Option[User], cond: Boolean): Fu[UserChat.Mine] = me match {
      case Some(user) if cond => findMine(chatId, user)
      case Some(user) => fuccess(UserChat.Mine(Chat.makeUser(chatId) forUser user.some, false))
      case None if cond => find(chatId) dmap { UserChat.Mine(_, false) }
      case None => fuccess(UserChat.Mine(Chat.makeUser(chatId), false))
    }

    private def findMine(chatId: Chat.Id, me: User): Fu[UserChat.Mine] = find(chatId) flatMap { chat =>
      (!chat.isEmpty ?? chatTimeout.isActive(chatId, me.id)) dmap {
        UserChat.Mine(chat forUser me.some, _)
      }
    }

    def write(chatId: Chat.Id, userId: String, text: String, publicSource: Option[PublicSource]): Funit =
      makeLine(chatId, userId, text) flatMap {
        _ ?? { line =>
          pushLine(chatId, line) >>- {
            if (publicSource.isDefined) cached invalidate chatId
            shutup ! {
              publicSource match {
                case Some(source) => RecordPublicChat(userId, text, source)
                case _ => RecordPrivateChat(chatId.value, userId, text)
              }
            }
            lilaBus.publish(actorApi.ChatLine(chatId, line), channelOf(chatId))
          }
        }
      }

    def clear(chatId: Chat.Id) = coll.remove($id(chatId)).void

    def system(chatId: Chat.Id, text: String): Funit = {
      val line = UserLine(systemUserId, text, troll = false, deleted = false)
      pushLine(chatId, line) >>-
        lilaBus.publish(actorApi.ChatLine(chatId, line), channelOf(chatId))
    }

    // like system, but not persisted.
    def volatile(chatId: Chat.Id, text: String): Unit = {
      val line = UserLine(systemUserId, text, troll = false, deleted = false)
      lilaBus.publish(actorApi.ChatLine(chatId, line), channelOf(chatId))
    }

    def timeout(chatId: Chat.Id, modId: String, userId: String, reason: ChatTimeout.Reason, local: Boolean): Funit =
      coll.byId[UserChat](chatId.value) zip UserRepo.byId(modId) zip UserRepo.byId(userId) flatMap {
        case Some(chat) ~ Some(mod) ~ Some(user) if isMod(mod) || local => doTimeout(chat, mod, user, reason)
        case _ => fuccess(none)
      }

    def userModInfo(username: String): Fu[Option[UserModInfo]] =
      UserRepo named username flatMap {
        _ ?? { user =>
          chatTimeout.history(user, 20) dmap { UserModInfo(user, _).some }
        }
      }

    private def doTimeout(c: UserChat, mod: User, user: User, reason: ChatTimeout.Reason): Funit = {
      val line = UserLine(
        username = systemUserId,
        text = s"${user.username} was timed out 10 minutes for ${reason.name}.",
        troll = false, deleted = false
      )
      val chat = c.markDeleted(user) add line
      coll.update($id(chat.id), chat).void >>
        chatTimeout.add(c, mod, user, reason) >>- {
          cached invalidate chat.id
          lilaBus.publish(actorApi.OnTimeout(user.username), channelOf(chat.id))
          lilaBus.publish(actorApi.ChatLine(chat.id, line), channelOf(chat.id))
          if (isMod(mod)) modLog ! lila.hub.actorApi.mod.ChatTimeout(
            mod = mod.id, user = user.id, reason = reason.key
          )
          else logger.info(s"${mod.username} times out ${user.username} in #${c.id} for ${reason.key}")
        }
    }

    def delete(c: UserChat, user: User): Funit = {
      val chat = c.markDeleted(user)
      coll.update($id(chat.id), chat).void >>- {
        cached invalidate chat.id
        lilaBus.publish(actorApi.OnTimeout(user.username), channelOf(chat.id))
      }
    }

    private def isMod(user: User) = lila.security.Granter(_.ChatTimeout)(user)

    def reinstate(list: List[ChatTimeout.Reinstate]) = list.foreach { r =>
      lilaBus.publish(actorApi.OnReinstate(r.user), Symbol(s"chat-${r.chat}"))
    }

    private[ChatApi] def makeLine(chatId: Chat.Id, userId: String, t1: String): Fu[Option[UserLine]] =
      UserRepo.byId(userId) zip chatTimeout.isActive(chatId, userId) dmap {
        case (Some(user), false) if !user.disabled => Writer cut t1 flatMap { t2 =>
          flood.allowMessage(user.id, t2) option
            UserLine(user.username, Writer preprocessUserInput t2, troll = user.troll, deleted = false)
        }
        case _ => none
      }
  }

  object playerChat {

    def findOption(chatId: Chat.Id): Fu[Option[MixedChat]] =
      coll.byId[MixedChat](chatId.value)

    def find(chatId: Chat.Id): Fu[MixedChat] =
      findOption(chatId) dmap (_ | Chat.makeMixed(chatId))

    def findIf(chatId: Chat.Id, cond: Boolean): Fu[MixedChat] =
      if (cond) find(chatId)
      else fuccess(Chat.makeMixed(chatId))

    def findNonEmpty(chatId: Chat.Id): Fu[Option[MixedChat]] =
      findOption(chatId) dmap (_ filter (_.nonEmpty))

    def optionsByOrderedIds(chatIds: List[Chat.Id]): Fu[List[Option[MixedChat]]] =
      coll.optionsByOrderedIds[MixedChat, Chat.Id](chatIds, ReadPreference.secondaryPreferred)(_.id)

    def write(chatId: Chat.Id, color: Color, text: String): Funit =
      makeLine(chatId, color, text) ?? { line =>
        pushLine(chatId, line) >>-
          lilaBus.publish(actorApi.ChatLine(chatId, line), channelOf(chatId))
      }

    private def makeLine(chatId: Chat.Id, color: Color, t1: String): Option[Line] =
      Writer cut t1 flatMap { t2 =>
        flood.allowMessage(s"$chatId/${color.letter}", t2) option
          PlayerLine(color, Writer preprocessUserInput t2)
      }
  }

  private[chat] def remove(chatId: Chat.Id) = coll.remove($id(chatId)).void

  private def pushLine(chatId: Chat.Id, line: Line): Funit = coll.update(
    $id(chatId),
    $doc("$push" -> $doc(
      Chat.BSONFields.lines -> $doc(
        "$each" -> List(line),
        "$slice" -> -maxLinesPerChat
      )
    )),
    upsert = true
  ).void >>- lila.mon.chat.message()

  private def channelOf(id: Chat.Id) = Symbol(s"chat-$id")

  private object Writer {

    import java.util.regex.Matcher.quoteReplacement

    def preprocessUserInput(in: String) = multiline(noShouting(noPrivateUrl(in)))

    def cut(text: String) = Some(text.trim take 140) filter (_.nonEmpty)

    private val domainRegex = netDomain.replace(".", """\.""")
    private val gameUrlRegex = (domainRegex + """\b/([\w]{8})[\w]{4}\b""").r
    private def noPrivateUrl(str: String): String =
      gameUrlRegex.replaceAllIn(str, m => quoteReplacement(netDomain + "/" + (m group 1)))
    private val multilineRegex = """\n{3,}""".r
    private def multiline(str: String) = multilineRegex.replaceAllIn(str, """\n\n""")
  }

  private object noShouting {
    import java.lang.Character.isUpperCase
    private val onlyLettersRegex = """[^\w]""".r
    def apply(text: String) = if (text.size < 5) text else {
      val onlyLetters = onlyLettersRegex.replaceAllIn(text take 80, "")
      if (onlyLetters.count(isUpperCase) > onlyLetters.size / 2)
        text.toLowerCase
      else text
    }
  }
}
