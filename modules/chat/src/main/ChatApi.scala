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
    spam: lila.security.Spam,
    shutup: akka.actor.ActorSelection,
    modLog: akka.actor.ActorSelection,
    asyncCache: lila.memo.AsyncCache.Builder,
    lilaBus: lila.common.Bus,
    maxLinesPerChat: Int,
    netDomain: String
) {

  import Chat.{ userChatBSONHandler, chatIdBSONHandler, classify }

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
            lilaBus.publish(actorApi.ChatLine(chatId, line), classify(chatId))
          }
        }
      }

    def clear(chatId: Chat.Id) = coll.remove($id(chatId)).void

    def system(chatId: Chat.Id, text: String): Funit = {
      val line = UserLine(systemUserId, None, text, troll = false, deleted = false)
      pushLine(chatId, line) >>-
        lilaBus.publish(actorApi.ChatLine(chatId, line), classify(chatId))
    }

    // like system, but not persisted.
    def volatile(chatId: Chat.Id, text: String): Unit = {
      val line = UserLine(systemUserId, None, text, troll = false, deleted = false)
      lilaBus.publish(actorApi.ChatLine(chatId, line), classify(chatId))
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
        title = None,
        text = s"${user.username} was timed out 10 minutes for ${reason.name}.",
        troll = false, deleted = false
      )
      val chat = c.markDeleted(user) add line
      coll.update($id(chat.id), chat).void >>
        chatTimeout.add(c, mod, user, reason) >>- {
          cached invalidate chat.id
          lilaBus.publish(actorApi.OnTimeout(user.username), classify(chat.id))
          lilaBus.publish(actorApi.ChatLine(chat.id, line), classify(chat.id))
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
        lilaBus.publish(actorApi.OnTimeout(user.username), classify(chat.id))
      }
    }

    private def isMod(user: User) = lila.security.Granter(_.ChatTimeout)(user)

    def reinstate(list: List[ChatTimeout.Reinstate]) = list.foreach { r =>
      lilaBus.publish(
        actorApi.OnReinstate(r.user),
        Chat classify Chat.Id(r.chat)
      )
    }

    private[ChatApi] def makeLine(chatId: Chat.Id, userId: String, t1: String): Fu[Option[UserLine]] =
      UserRepo.speaker(userId) zip chatTimeout.isActive(chatId, userId) dmap {
        case (Some(user), false) if user.enabled => Writer cut t1 flatMap { t2 =>
          (user.isBot || flood.allowMessage(userId, t2)) option
            UserLine(user.username, user.title.map(_.value), Writer preprocessUserInput t2, troll = ~user.troll, deleted = false)
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
          lilaBus.publish(actorApi.ChatLine(chatId, line), classify(chatId))
      }

    private def makeLine(chatId: Chat.Id, color: Color, t1: String): Option[Line] =
      Writer cut t1 flatMap { t2 =>
        flood.allowMessage(s"$chatId/${color.letter}", t2) option
          PlayerLine(color, Writer preprocessUserInput t2)
      }
  }

  private[chat] def remove(chatId: Chat.Id) = coll.remove($id(chatId)).void

  private[chat] def removeAll(chatIds: List[Chat.Id]) = coll.remove($inIds(chatIds)).void

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

  private object Writer {

    import java.util.regex.{ Pattern, Matcher }

    def preprocessUserInput(in: String) = multiline(spam.replace(noShouting(noPrivateUrl(in))))

    def cut(text: String) = Some(text.trim take Line.textMaxSize) filter (_.nonEmpty)

    private val gameUrlRegex = (Pattern.quote(netDomain) + """\b/(\w{8})\w{4}\b""").r
    private val gameUrlReplace = Matcher.quoteReplacement(netDomain) + "/$1";
    private def noPrivateUrl(str: String): String = gameUrlRegex.replaceAllIn(str, gameUrlReplace)
    private def noShouting(str: String): String = if (isShouting(str)) str.toLowerCase else str
    private val multilineRegex = """\n\n{2,}+""".r
    private def multiline(str: String) = multilineRegex.replaceAllIn(str, """\n\n""")
  }

  private def isShouting(text: String) = text.length >= 5 && {
    import java.lang.Character._
    // true if >1/2 of the latin letters are uppercase
    (text take 80).foldLeft(0) { (i, c) =>
      getType(c) match {
        case UPPERCASE_LETTER => i + 1
        case LOWERCASE_LETTER => i - 1
        case _ => i
      }
    } > 0
  }
}
