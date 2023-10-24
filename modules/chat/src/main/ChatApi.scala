package lila.chat

import chess.Color

import lila.common.Bus
import lila.common.config.NetDomain
import lila.common.String.{ fullCleanUp, noShouting }
import lila.security.{ Flood, Granter }
import lila.db.dsl.{ *, given }
import lila.hub.actorApi.shutup.{ PublicSource, RecordPrivateChat, RecordPublicText }
import lila.memo.CacheApi.*
import lila.user.{ Me, User, UserRepo }

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

      private val cache = cacheApi[ChatId, UserChat](1024, "chat.user"):
        _.expireAfterWrite(1 minute).buildAsyncFuture(find)

      def invalidate = cache.invalidate

      def findMine(chatId: ChatId)(using me: Option[Me]): Fu[UserChat.Mine] =
        me match
          case Some(me) => findMine(chatId)(using me)
          case None     => cache.get(chatId) dmap { UserChat.Mine(_, timeout = false) }

      private def findMine(chatId: ChatId)(using me: Me): Fu[UserChat.Mine] =
        cache get chatId flatMap { chat =>
          (!chat.isEmpty so chatTimeout.isActive(chatId, me)) dmap {
            UserChat.Mine(chat forUser me.some, _)
          }
        }

    def findOption(chatId: ChatId): Fu[Option[UserChat]] =
      coll.byId[UserChat](chatId.value)

    def find(chatId: ChatId): Fu[UserChat] =
      findOption(chatId).dmap(_ | Chat.makeUser(chatId))

    def findAll(chatIds: List[ChatId]): Fu[List[UserChat]] =
      coll.byStringIds[UserChat](ChatId raw chatIds, _.sec)

    def findMine(chatId: ChatId)(using Option[Me]): Fu[UserChat.Mine] = findMineIf(chatId, cond = true)

    def findMineIf(chatId: ChatId, cond: Boolean)(using me: Option[Me]): Fu[UserChat.Mine] =
      me match
        case Some(me) if cond => findMine(chatId)(using me)
        case Some(me)     => fuccess(UserChat.Mine(Chat.makeUser(chatId) forUser me.some, timeout = false))
        case None if cond => find(chatId) dmap { UserChat.Mine(_, timeout = false) }
        case None         => fuccess(UserChat.Mine(Chat.makeUser(chatId), timeout = false))

    private def findMine(chatId: ChatId)(using me: Me): Fu[UserChat.Mine] =
      find(chatId) flatMap { chat =>
        (!chat.isEmpty so chatTimeout.isActive(chatId, me)) dmap {
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
        isChatFresh(publicSource) flatMap {
          if _ then
            linkCheck(line, publicSource) flatMap {
              if _ then
                (persist so persistLine(chatId, line)).andDo:
                  if persist then
                    if publicSource.isDefined then cached invalidate chatId
                    shutup ! publicSource.match
                      case Some(source) => RecordPublicText(userId, text, source)
                      case _            => RecordPrivateChat(chatId.value, userId, text)
                    lila.mon.chat
                      .message(publicSource.fold("player")(_.parentName), line.troll)
                      .increment()

                  publish(chatId, ChatLine(chatId, line), busChan)
              else
                logger.info(s"Link check rejected $line in $publicSource")
                funit
            }
          else
            logger.info(s"Can't post $line in $publicSource: chat is closed")
            funit
        }
      }

    private def linkCheck(line: UserLine, source: Option[PublicSource]) =
      source.fold(fuccess(true)): s =>
        Bus.ask("chatLinkCheck") { GetLinkCheck(line, s, _) }

    private object isChatFresh:
      private val cache = cacheApi[PublicSource, Boolean](256, "chat.fresh"):
        _.expireAfterWrite(2.minutes).buildAsyncFuture: source =>
          Bus.ask("chatFreshness") { IsChatFresh(source, _) }
      def apply(source: Option[PublicSource]) =
        source.fold(fuccess(true))(cache.get)

    def clear(chatId: ChatId) = coll.delete.one($id(chatId)).void

    def system(chatId: ChatId, text: String, busChan: BusChan.Select): Funit =
      val line = UserLine(User.lichessName, None, false, text, troll = false, deleted = false)
      persistLine(chatId, line).andDo:
        cached.invalidate(chatId)
        publish(chatId, ChatLine(chatId, line), busChan)

    // like system, but not persisted.
    def volatile(chatId: ChatId, text: String, busChan: BusChan.Select): Unit =
      val line = UserLine(User.lichessName, None, false, text, troll = false, deleted = false)
      publish(chatId, ChatLine(chatId, line), busChan)

    def service(chatId: ChatId, text: String, busChan: BusChan.Select, isVolatile: Boolean): Unit =
      (if isVolatile then volatile else system) (chatId, text, busChan)

    def timeout(
        chatId: ChatId,
        userId: UserId,
        reason: ChatTimeout.Reason,
        scope: ChatTimeout.Scope,
        text: String,
        busChan: BusChan.Select
    )(using mod: Me.Id): Funit =
      coll.byId[UserChat](chatId.value) zip userRepo.me(mod) zip userRepo.byId(userId) flatMap {
        case ((Some(chat), Some(me)), Some(user))
            if isMod(me) || (busChan(BusChan) == BusChan.Study && isRelayMod(me)) ||
              scope == ChatTimeout.Scope.Local =>
          doTimeout(chat, me, user, reason, scope, text, busChan)
        case _ => funit
      }

    def publicTimeout(data: ChatTimeout.TimeoutFormData)(using Me): Funit =
      ChatTimeout.Reason(data.reason) so { reason =>
        timeout(
          chatId = data.roomId into ChatId,
          userId = data.userId.id,
          reason = reason,
          scope = ChatTimeout.Scope.Global,
          text = data.text,
          busChan = data.chan match
            case "tournament" => _.Tournament
            case "swiss"      => _.Swiss
            case "team"       => _.Team
            case _            => _.Study
        )
      }

    def userModInfo(username: UserStr): Fu[Option[UserModInfo]] =
      userRepo byId username flatMapz { user =>
        chatTimeout.history(user, 20) dmap { UserModInfo(user, _).some }
      }

    private def doTimeout(
        c: UserChat,
        mod: Me,
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
        coll.update.one($id(chat.id), chat).void andDo {
          cached.invalidate(chat.id)
          publish(chat.id, OnTimeout(chat.id, user.id), busChan)
          line.foreach: l =>
            publish(chat.id, ChatLine(chat.id, l), busChan)
          if isMod(mod) || isRelayMod(mod) then
            lila.common.Bus.publish(
              lila.hub.actorApi.mod.ChatTimeout(
                mod = mod.userId,
                user = user.id,
                reason = reason.key,
                text = text
              ),
              "chatTimeout"
            )
            if isNew then
              lila.common.Bus
                .publish(lila.hub.actorApi.security.DeletePublicChats(user.id), "deletePublicChats")
          else logger.info(s"${mod.username} times out ${user.username} in #${c.id} for ${reason.key}")
        }
      }

    def delete(c: UserChat, user: User, busChan: BusChan.Select): Fu[Boolean] =
      val chat   = c.markDeleted(user)
      val change = chat != c
      change.so {
        coll.update.one($id(chat.id), chat).void andDo {
          cached invalidate chat.id
          publish(chat.id, OnTimeout(chat.id, user.id), busChan)
        }
      } inject change

    private def isMod(me: Me)      = Granter(_.ChatTimeout)(using me)
    private def isRelayMod(me: Me) = Granter(_.BroadcastTimeout)(using me)

    def reinstate(list: List[ChatTimeout.Reinstate]) =
      list.foreach: r =>
        Bus.publish(OnReinstate(r.chat, r.user), BusChan.Global.chan)

    private[ChatApi] def makeLine(chatId: ChatId, userId: UserId, t1: String): Fu[Option[UserLine]] =
      userRepo.speaker(userId) zip chatTimeout.isActive(chatId, userId) dmap {
        case (Some(user), false) if user.enabled =>
          Writer.preprocessUserInput(t1, user.username.some) flatMap { t2 =>
            val allow =
              if user.isBot then !lila.common.String.hasLinks(t2)
              else flood.allowMessage(userId into Flood.Source, t2)
            allow option UserLine(
              user.username,
              user.title,
              user.isPatron,
              t2,
              troll = user.isTroll,
              deleted = false
            )
          }
        case _ => none
      }

  object playerChat:

    def findOption(chatId: ChatId): Fu[Option[MixedChat]] =
      coll.byId[MixedChat](chatId.value)

    def find(chatId: ChatId): Fu[MixedChat] =
      findOption(chatId) dmap (_ | Chat.makeMixed(chatId))

    def findIf(chatId: ChatId, cond: Boolean): Fu[MixedChat] =
      if cond then find(chatId)
      else fuccess(Chat.makeMixed(chatId))

    def findNonEmpty(chatId: ChatId): Fu[Option[MixedChat]] =
      findOption(chatId) dmap (_ filter (_.nonEmpty))

    def optionsByOrderedIds(chatIds: List[ChatId]): Fu[List[Option[MixedChat]]] =
      coll.optionsByOrderedIds[MixedChat, ChatId](chatIds, none, _.sec)(_.id)

    def write(chatId: ChatId, color: Color, text: String, busChan: BusChan.Select): Funit =
      makeLine(chatId, color, text).so: line =>
        persistLine(chatId, line).andDo:
          publish(chatId, ChatLine(chatId, line), busChan)
          lila.mon.chat.message("anonPlayer", troll = false).increment()

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
      val out1 = multiline:
        spam.replace(noShouting(noPrivateUrl(fullCleanUp(in))))
      val out2 = username.fold(out1) { removeSelfMention(out1, _) }
      out2.take(Line.textMaxSize).some.filter(_.nonEmpty)

    private def removeSelfMention(in: String, username: UserName) =
      if in.contains('@') then
        ("""(?i)@(?<![\w@#/]@)""" + username + """(?![@\w-]|\.\w)""").r.replaceAllIn(in, username.value)
      else in

    private val gameUrlRegex   = (Pattern.quote(netDomain.value) + """\b/(\w{8})\w{4}\b""").r
    private val gameUrlReplace = Matcher.quoteReplacement(netDomain.value) + "/$1"

    private def noPrivateUrl(str: String): String = gameUrlRegex.replaceAllIn(str, gameUrlReplace)
    private val multilineRegex                    = """\n\n{1,}+""".r
    private def multiline(str: String)            = multilineRegex.replaceAllIn(str, " ")
