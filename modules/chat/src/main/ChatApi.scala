package lila.chat

import chess.Color
import reactivemongo.api.ReadPreference

import lila.common.Bus
import lila.common.config.NetDomain
import lila.common.String.{ fullCleanUp, noShouting }
import lila.security.Flood
import lila.db.dsl.{ *, given }
import lila.hub.actorApi.shutup.{ PublicSource, RecordPrivateChat, RecordPublicChat }
import lila.memo.CacheApi.*
import lila.user.{ Holder, User, UserRepo }

final class ChatApi(
    coll: Coll,
    userRepo: UserRepo,
    chatTimeout: ChatTimeout,
    flood: Flood,
    spam: lila.security.Spam,
    shutup: lila.hub.actors.Shutup,
    cacheApi: lila.memo.CacheApi,
    netDomain: NetDomain
)(using Executor, Scheduler):

  import Chat.given

  def exists(id: ChatId) = coll.exists($id(id))

  object userChat:

    // only use for public, multi-user chats - tournaments, simuls
    object cached:

      private val cache = cacheApi[ChatId, UserChat](1024, "chat.user") {
        _.expireAfterWrite(1 minute).buildAsyncFuture(find)
      }

      def invalidate = cache.invalidate

      def findMine(chatId: ChatId, me: Option[User]): Fu[UserChat.Mine] =
        me match
          case Some(user) => findMine(chatId, user)
          case None       => cache.get(chatId) dmap { UserChat.Mine(_, timeout = false) }

      private def findMine(chatId: ChatId, me: User): Fu[UserChat.Mine] =
        cache get chatId flatMap { chat =>
          (!chat.isEmpty ?? chatTimeout.isActive(chatId, me.id)) dmap {
            UserChat.Mine(chat forUser me.some, _)
          }
        }

    def findOption(chatId: ChatId): Fu[Option[UserChat]] =
      coll.byId[UserChat](chatId.value)

    def find(chatId: ChatId): Fu[UserChat] =
      findOption(chatId) dmap (_ | Chat.makeUser(chatId))

    def findAll(chatIds: List[ChatId]): Fu[List[UserChat]] =
      coll.byStringIds[UserChat](ChatId raw chatIds, ReadPreference.secondaryPreferred)

    def findMine(chatId: ChatId, me: Option[User]): Fu[UserChat.Mine] = findMineIf(chatId, me, cond = true)

    def findMineIf(chatId: ChatId, me: Option[User], cond: Boolean): Fu[UserChat.Mine] =
      me match
        case Some(user) if cond => findMine(chatId, user)
        case Some(user)   => fuccess(UserChat.Mine(Chat.makeUser(chatId) forUser user.some, timeout = false))
        case None if cond => find(chatId) dmap { UserChat.Mine(_, timeout = false) }
        case None         => fuccess(UserChat.Mine(Chat.makeUser(chatId), timeout = false))

    private def findMine(chatId: ChatId, me: User): Fu[UserChat.Mine] =
      find(chatId) flatMap { chat =>
        (!chat.isEmpty ?? chatTimeout.isActive(chatId, me.id)) dmap {
          UserChat.Mine(chat forUser me.some, _)
        }
      }

    def write(
        chatId: ChatId,
        userId: UserId,
        text: String,
        publicSource: Option[PublicSource],
        busChan: BusChan.Select,
        persist: Boolean = true
    ): Funit =
      makeLine(chatId, userId, text) flatMapz { line =>
        linkCheck(line, publicSource) flatMap {
          case false =>
            logger.info(s"Link check rejected $line in $publicSource")
            funit
          case true =>
            (persist ?? persistLine(chatId, line)) >>- {
              if (persist)
                if (publicSource.isDefined) cached invalidate chatId
                shutup ! {
                  publicSource match
                    case Some(source) => RecordPublicChat(userId, text, source)
                    case _            => RecordPrivateChat(chatId.value, userId, text)
                }
                lila.mon.chat
                  .message(publicSource.fold("player")(_.parentName), line.troll)
                  .increment()
                  .unit
              publish(chatId, ChatLine(chatId, line), busChan)
            }
        }
      }

    private def linkCheck(line: UserLine, source: Option[PublicSource]) =
      source.fold(fuccess(true)) { s =>
        Bus.ask("chatLinkCheck") { GetLinkCheck(line, s, _) }
      }

    def clear(chatId: ChatId) = coll.delete.one($id(chatId)).void

    def system(chatId: ChatId, text: String, busChan: BusChan.Select): Funit =
      val line = UserLine(User.lichessName, None, false, text, troll = false, deleted = false)
      persistLine(chatId, line) >>- {
        cached.invalidate(chatId)
        publish(chatId, ChatLine(chatId, line), busChan)
      }

    // like system, but not persisted.
    def volatile(chatId: ChatId, text: String, busChan: BusChan.Select): Unit =
      val line = UserLine(User.lichessName, None, false, text, troll = false, deleted = false)
      publish(chatId, ChatLine(chatId, line), busChan)

    def service(chatId: ChatId, text: String, busChan: BusChan.Select, isVolatile: Boolean): Unit =
      (if (isVolatile) volatile else system) (chatId, text, busChan).unit

    def timeout(
        chatId: ChatId,
        modId: UserId,
        userId: UserId,
        reason: ChatTimeout.Reason,
        scope: ChatTimeout.Scope,
        text: String,
        busChan: BusChan.Select
    ): Funit =
      coll.byId[UserChat](chatId.value) zip userRepo.byId(modId) zip userRepo.byId(userId) flatMap {
        case ((Some(chat), Some(mod)), Some(user))
            if isMod(mod) || (busChan(BusChan) == BusChan.Study && isRelayMod(
              mod
            )) || scope == ChatTimeout.Scope.Local =>
          doTimeout(chat, mod, user, reason, scope, text, busChan)
        case _ => funit
      }

    def publicTimeout(data: ChatTimeout.TimeoutFormData, me: Holder): Funit =
      ChatTimeout.Reason(data.reason) ?? { reason =>
        timeout(
          chatId = data.roomId into ChatId,
          modId = me.id,
          userId = data.userId.id,
          reason = reason,
          scope = ChatTimeout.Scope.Global,
          text = data.text,
          busChan = data.chan match {
            case "tournament" => _.Tournament
            case "swiss"      => _.Swiss
            case "team"       => _.Team
            case _            => _.Study
          }
        )
      }

    def userModInfo(username: UserStr): Fu[Option[UserModInfo]] =
      userRepo byId username flatMapz { user =>
        chatTimeout.history(user, 20) dmap { UserModInfo(user, _).some }
      }

    private def doTimeout(
        c: UserChat,
        mod: User,
        user: User,
        reason: ChatTimeout.Reason,
        scope: ChatTimeout.Scope,
        text: String,
        busChan: BusChan.Select
    ): Funit =
      chatTimeout.add(c, mod, user, reason, scope) flatMap { isNew =>
        val lineText = scope match
          case ChatTimeout.Scope.Global => s"${user.username} was timed out 15 minutes for ${reason.name}."
          case _ => s"${user.username} was timed out 15 minutes by a page mod (not a Lichess mod)"
        val line = (isNew && c.hasRecentLine(user)) option UserLine(
          username = User.lichessName,
          title = None,
          patron = user.isPatron,
          text = lineText,
          troll = false,
          deleted = false
        )
        val c2   = c.markDeleted(user)
        val chat = line.fold(c2)(c2.add)
        coll.update.one($id(chat.id), chat).void >>- {
          cached.invalidate(chat.id)
          publish(chat.id, OnTimeout(chat.id, user.id), busChan)
          line foreach { l =>
            publish(chat.id, ChatLine(chat.id, l), busChan)
          }
          if (scope == ChatTimeout.Scope.Global)
            lila.common.Bus.publish(
              lila.hub.actorApi.mod.ChatTimeout(
                mod = mod.id,
                user = user.id,
                reason = reason.key,
                text = text
              ),
              "chatTimeout"
            )
            if (isNew)
              lila.common.Bus
                .publish(lila.hub.actorApi.security.DeletePublicChats(user.id), "deletePublicChats")
          else logger.info(s"${mod.username} times out ${user.username} in #${c.id} for ${reason.key}")
        }
      }

    def delete(c: UserChat, user: User, busChan: BusChan.Select): Fu[Boolean] =
      val chat   = c.markDeleted(user)
      val change = chat != c
      change.?? {
        coll.update.one($id(chat.id), chat).void >>- {
          cached invalidate chat.id
          publish(chat.id, OnTimeout(chat.id, user.id), busChan)
        }
      } inject change

    private def isMod(user: User)      = lila.security.Granter(_.ChatTimeout)(user)
    private def isRelayMod(user: User) = lila.security.Granter(_.BroadcastTimeout)(user)

    def reinstate(list: List[ChatTimeout.Reinstate]) =
      list.foreach { r =>
        Bus.publish(OnReinstate(r.chat, r.user), BusChan.Global.chan)
      }

    private[ChatApi] def makeLine(chatId: ChatId, userId: UserId, t1: String): Fu[Option[UserLine]] =
      userRepo.speaker(userId) zip chatTimeout.isActive(chatId, userId) dmap {
        case (Some(user), false) if user.enabled =>
          Writer.preprocessUserInput(t1, user.username.some) flatMap { t2 =>
            val allow =
              if (user.isBot) !lila.common.String.hasLinks(t2)
              else flood.allowMessage(userId into Flood.Source, t2)
            allow option {
              UserLine(
                user.username,
                user.title,
                user.isPatron,
                t2,
                troll = user.isTroll,
                deleted = false
              )
            }
          }
        case _ => none
      }

  object playerChat:

    def findOption(chatId: ChatId): Fu[Option[MixedChat]] =
      coll.byId[MixedChat](chatId.value)

    def find(chatId: ChatId): Fu[MixedChat] =
      findOption(chatId) dmap (_ | Chat.makeMixed(chatId))

    def findIf(chatId: ChatId, cond: Boolean): Fu[MixedChat] =
      if (cond) find(chatId)
      else fuccess(Chat.makeMixed(chatId))

    def findNonEmpty(chatId: ChatId): Fu[Option[MixedChat]] =
      findOption(chatId) dmap (_ filter (_.nonEmpty))

    def optionsByOrderedIds(chatIds: List[ChatId]): Fu[List[Option[MixedChat]]] =
      coll.optionsByOrderedIds[MixedChat, ChatId](chatIds, none, ReadPreference.secondaryPreferred)(_.id)

    def write(chatId: ChatId, color: Color, text: String, busChan: BusChan.Select): Funit =
      makeLine(chatId, color, text) ?? { line =>
        persistLine(chatId, line) >>- {
          publish(chatId, ChatLine(chatId, line), busChan)
          lila.mon.chat.message("anonPlayer", troll = false).increment().unit
        }
      }

    private def makeLine(chatId: ChatId, color: Color, t1: String): Option[Line] =
      Writer.preprocessUserInput(t1, none) flatMap { t2 =>
        flood.allowMessage(Flood Source s"$chatId/${color.letter}", t2) option
          PlayerLine(color, t2)
      }

  private def publish(chatId: ChatId, msg: Any, busChan: BusChan.Select): Unit =
    Bus.publish(msg, busChan(BusChan).chan)
    Bus.publish(msg, Chat chanOf chatId)

  def remove(chatId: ChatId) = coll.delete.one($id(chatId)).void

  def removeAll(chatIds: List[ChatId]) = coll.delete.one($inIds(chatIds)).void

  private def persistLine(chatId: ChatId, line: Line): Funit =
    coll.update
      .one(
        $id(chatId),
        $doc(
          "$push" -> $doc(
            Chat.BSONFields.lines -> $doc(
              "$each"  -> List(line),
              "$slice" -> -200
            )
          )
        ),
        upsert = true
      )
      .void

  private object Writer:

    import java.util.regex.{ Matcher, Pattern }

    def preprocessUserInput(in: String, username: Option[UserName]): Option[String] =
      val out1 = multiline(
        spam.replace(noShouting(noPrivateUrl(fullCleanUp(in))))
      )
      val out2 = username.fold(out1) { removeSelfMention(out1, _) }
      out2.take(Line.textMaxSize).some.filter(_.nonEmpty)

    private def removeSelfMention(in: String, username: UserName) =
      if (in.contains('@'))
        ("""(?i)@(?<![\w@#/]@)""" + username + """(?![@\w-]|\.\w)""").r.replaceAllIn(in, username.value)
      else in

    private val gameUrlRegex   = (Pattern.quote(netDomain.value) + """\b/(\w{8})\w{4}\b""").r
    private val gameUrlReplace = Matcher.quoteReplacement(netDomain.value) + "/$1"

    private def noPrivateUrl(str: String): String = gameUrlRegex.replaceAllIn(str, gameUrlReplace)
    private val multilineRegex                    = """\n\n{1,}+""".r
    private def multiline(str: String)            = multilineRegex.replaceAllIn(str, " ")
