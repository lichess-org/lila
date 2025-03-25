package lila.chat

import lila.common.Bus
import lila.common.String.{ fullCleanUp, noShouting }
import lila.core.chat.{ OnReinstate, OnTimeout }
import lila.core.config.NetDomain
import lila.core.perm.Granter
import lila.core.security.{ FloodApi, FloodSource, SpamApi }
import lila.core.shutup.PublicSource
import lila.core.user.{ FlairGet, FlairGetMap }
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi.*

final class ChatApi(
    coll: Coll,
    userApi: lila.core.user.UserApi,
    userRepo: lila.core.user.UserRepo,
    chatTimeout: ChatTimeout,
    flood: FloodApi,
    spam: SpamApi,
    shutupApi: lila.core.shutup.ShutupApi,
    cacheApi: lila.memo.CacheApi,
    netDomain: NetDomain
)(using Executor, Scheduler, FlairGet, FlairGetMap)
    extends lila.core.chat.ChatApi:

  import Chat.given
  export userChat.{ write, volatile, timeout, system }

  def exists(id: ChatId) = coll.exists($id(id))

  object userChat:

    // only use for public, multi-user chats - tournaments, simuls
    object cached:

      private val cache = cacheApi[ChatId, UserChat](1024, "chat.user"):
        _.expireAfterWrite(1.minute).buildAsyncFuture(find)

      def invalidate = cache.invalidate

      def findMine(chatId: ChatId)(using Option[Me], AllMessages): Fu[UserChat.Mine] =
        cache.get(chatId).flatMap(makeMine)

    def findOption(chatId: ChatId): Fu[Option[UserChat]] =
      coll.byId[UserChat](chatId.value)

    def find(chatId: ChatId): Fu[UserChat] =
      findOption(chatId).dmap(_ | Chat.makeUser(chatId))

    def findAll(chatIds: List[ChatId]): Fu[List[UserChat]] =
      coll.byStringIds[UserChat](ChatId.raw(chatIds), _.sec)

    def findMine(chatId: ChatId, cond: Boolean = true)(using Option[Me], AllMessages): Fu[UserChat.Mine] =
      if cond then find(chatId).flatMap(makeMine)
      else fuccess(UserChat.Mine(Chat.makeUser(chatId), JsonChatLines.empty, timeout = false))

    private def makeMine(chat: UserChat)(using me: Option[Me], all: AllMessages): Fu[UserChat.Mine] =
      val mine = chat.forMe
      for
        lines <- JsonView.asyncLines(mine)
        timeout <- me
          .ifFalse(mine.isEmpty)
          .so:
            chatTimeout.isActive(chat.id, _)
      yield UserChat.Mine(mine, lines, timeout)

    def write(
        chatId: ChatId,
        userId: UserId,
        text: String,
        publicSource: Option[PublicSource],
        busChan: BusChan.Select,
        persist: Boolean = true
    ): Funit =
      makeLine(chatId, userId, text).flatMapz: line =>
        isChatFresh(publicSource).flatMap:
          if _ then
            linkCheck(line, publicSource).flatMap:
              if _ then
                val actuallyPersist = persist && (publicSource.isEmpty || !isGarbage(text))
                for _ <- actuallyPersist.so(persistLine(chatId, line))
                yield
                  if actuallyPersist then
                    if publicSource.isDefined then cached.invalidate(chatId)
                    publicSource.match
                      case Some(source) => shutupApi.publicText(userId, text, source)
                      case _            => shutupApi.privateChat(chatId.value, userId, text)
                    lila.mon.chat
                      .message(publicSource.fold("player")(_.typeName), line.troll)
                      .increment()
                  publishLine(chatId, line, busChan)
              else
                logger.info(s"Link check rejected $line in $publicSource")
                funit
          else
            logger.info(s"Can't post $line in $publicSource: chat is closed")
            funit

    private def isGarbage(text: String) = {
      val x = text.filter(_.isLetter).toLowerCase
      x == "last" || x == "first" || x == "second" || x == "third"
    } || {
      val x = text.filter(_.isLetterOrDigit).toLowerCase
      x == "1st" || x == "1" || x == "2nd" || x == "2"
    }

    private def linkCheck(line: UserLine, source: Option[PublicSource]) =
      source.fold(fuccess(true)): s =>
        Bus.ask("chatLinkCheck") { GetLinkCheck(line, s, _) }

    private object isChatFresh:
      private val cache = cacheApi[PublicSource, Boolean](256, "chat.fresh"):
        _.expireAfterWrite(3.minutes).buildAsyncFuture: source =>
          Bus.ask("chatFreshness") { IsChatFresh(source, _) }
      def apply(source: Option[PublicSource]) =
        source.fold(fuccess(true))(cache.get)

    def clear(chatId: ChatId) = coll.delete.one($id(chatId)).void

    def system(chatId: ChatId, text: String, busChan: BusChan.Select): Funit =
      val line = UserLine(UserName.lichess, None, false, flair = true, text, troll = false, deleted = false)
      for _ <- persistLine(chatId, line)
      yield
        cached.invalidate(chatId)
        publishLine(chatId, line, busChan)

    // like system, but not persisted.
    def volatile(chatId: ChatId, text: String, busChan: BusChan.Select): Unit =
      val line = UserLine(UserName.lichess, None, false, flair = true, text, troll = false, deleted = false)
      publishLine(chatId, line, busChan)

    def timeout(
        chatId: ChatId,
        userId: UserId,
        reason: ChatTimeout.Reason,
        scope: ChatTimeout.Scope,
        text: String,
        busChan: BusChan.Select
    )(using mod: MyId): Funit =
      coll
        .byId[UserChat](chatId.value)
        .zip(userApi.me(mod))
        .zip(userApi.byId(userId))
        .flatMap:
          case ((Some(chat), Some(me)), Some(user))
              if isMod(using me) || (busChan(BusChan) == BusChan.study && isRelayMod(using me)) ||
                scope == ChatTimeout.Scope.Local =>
            doTimeout(chat, user, reason, scope, text, busChan)(using me)
          case _ => funit

    def publicTimeout(data: ChatTimeout.TimeoutFormData)(using MyId): Funit =
      ChatTimeout
        .Reason(data.reason)
        .so: reason =>
          timeout(
            chatId = data.roomId.into(ChatId),
            userId = data.userId.id,
            reason = reason,
            scope = ChatTimeout.Scope.Global,
            text = data.text,
            busChan = data.chan match
              case "tournament" => _.tournament
              case "swiss"      => _.swiss
              case "team"       => _.team
              case _            => _.study
          )

    def userModInfo(username: UserStr): Fu[Option[UserModInfo]] =
      userApi
        .byId(username)
        .flatMapz: user =>
          chatTimeout.history(user, 20).dmap { UserModInfo(user, _).some }

    private def doTimeout(
        c: UserChat,
        user: User,
        reason: ChatTimeout.Reason,
        scope: ChatTimeout.Scope,
        text: String,
        busChan: BusChan.Select
    )(using mod: Me): Funit =
      chatTimeout
        .add(c, mod, user, reason, scope)
        .flatMap: isNew =>
          val lineText = scope match
            case ChatTimeout.Scope.Global => s"${user.username} was timed out 15 minutes for ${reason.name}."
            case _ => s"${user.username} was timed out 15 minutes by a page mod (not a Lichess mod)"
          val line = (isNew && c.hasRecentLine(user)).option(
            UserLine(
              username = UserName.lichess,
              title = None,
              patron = false,
              flair = true,
              text = lineText,
              troll = false,
              deleted = false
            )
          )
          val c2   = c.markDeleted(user)
          val chat = line.fold(c2)(c2.add)
          for _ <- coll.update.one($id(chat.id), chat)
          yield
            cached.invalidate(chat.id)
            publish(chat.id, OnTimeout(chat.id, user.id), busChan)
            line.foreach: l =>
              publishLine(chat.id, l, busChan)
            if isMod || isRelayMod then
              lila.common.Bus.publish(
                lila.core.mod.ChatTimeout(
                  mod = mod.userId,
                  user = user.id,
                  reason = reason,
                  text = text
                ),
                "chatTimeout"
              )
              if isNew then
                lila.common.Bus.publish(lila.core.security.DeletePublicChats(user.id), "deletePublicChats")
            else logger.info(s"${mod.username} times out ${user.username} in #${c.id} for ${reason.key}")

    def delete(c: UserChat, user: User, busChan: BusChan.Select): Fu[Boolean] =
      val chat   = c.markDeleted(user)
      val change = chat != c
      change
        .so:
          for _ <- coll.update.one($id(chat.id), chat)
          yield
            cached.invalidate(chat.id)
            publish(chat.id, OnTimeout(chat.id, user.id), busChan)
        .inject(change)

    private def isMod(using Me)      = Granter(_.ChatTimeout)
    private def isRelayMod(using Me) = Granter(_.BroadcastTimeout)

    def reinstate(list: List[ChatTimeout.Reinstate]) =
      list.foreach: r =>
        Bus.publish(OnReinstate(r.chat, r.user), BusChan.global.chan)

    private[ChatApi] def makeLine(chatId: ChatId, userId: UserId, t1: String): Fu[Option[UserLine]] =
      Speaker
        .get(userId)
        .zip(chatTimeout.isActive(chatId, userId))
        .dmap:
          case (Some(user), false) if user.enabled =>
            Writer.preprocessUserInput(t1, user.username.some).flatMap { t2 =>
              val allow =
                if user.isBot then !lila.common.String.hasLinks(t2)
                else flood.allowMessage(userId.into(FloodSource), t2)
              allow.option(
                UserLine(
                  user.username,
                  user.title,
                  patron = user.isPatron,
                  flair = user.flair.isDefined,
                  t2,
                  troll = user.isTroll,
                  deleted = false
                )
              )
            }
          case _ => none

    def removeMessagesBy(gameIds: Seq[GameId], userId: UserId) =
      val regex  = s"^$userId[" + Line.separatorChars.mkString("") + "]"
      val update = $pull("l".$regex(regex, "i"))
      val allIds = for
        id   <- gameIds
        both <- List(id.value, s"${id.value}/w")
      yield both
      coll.update.one($inIds(allIds), update, multi = true).void

  private object Speaker:
    def get(userId: UserId): Fu[Option[Speaker]] = userApi.byIdAs[Speaker](userId.value, Speaker.projection)
    import lila.core.user.BSONFields as F
    val projection = lila.db.dsl.$doc(
      F.username -> true,
      F.title    -> true,
      F.plan     -> true,
      F.flair    -> true,
      F.enabled  -> true,
      F.marks    -> true
    )
    import reactivemongo.api.bson.*
    import userRepo.given
    given BSONDocumentHandler[Speaker] = Macros.handler[Speaker]

  object playerChat:

    def findOption(chatId: ChatId): Fu[Option[MixedChat]] =
      coll.byId[MixedChat](chatId.value)

    def find(chatId: ChatId): Fu[MixedChat] =
      findOption(chatId).dmap(_ | Chat.makeMixed(chatId))

    def findIf(chatId: ChatId, cond: Boolean): Fu[MixedChat] =
      if cond then find(chatId)
      else fuccess(Chat.makeMixed(chatId))

    def findNonEmpty(chatId: ChatId): Fu[Option[MixedChat]] =
      findOption(chatId).dmap(_.filter(_.nonEmpty))

    def optionsByOrderedIds(chatIds: List[ChatId]): Fu[List[Option[MixedChat]]] =
      coll.optionsByOrderedIds[MixedChat, ChatId](chatIds, none, _.sec)(_.id)

    def write(chatId: ChatId, color: Color, text: String, busChan: BusChan.Select): Funit =
      makeLine(chatId, color, text).so: line =>
        for _ <- persistLine(chatId, line) yield
          publishLine(chatId, line, busChan)
          lila.mon.chat.message("anonPlayer", troll = false).increment()

    private def makeLine(chatId: ChatId, color: Color, t1: String): Option[Line] =
      Writer
        .preprocessUserInput(t1, none)
        .flatMap: t2 =>
          flood.allowMessage(FloodSource(s"$chatId/${color.letter}"), t2).option(PlayerLine(color, t2))

  private def publish(chatId: ChatId, msg: Any, busChan: BusChan.Select): Unit =
    Bus.publish(msg, busChan(BusChan).chan)
    Bus.publish(msg, Chat.chanOf(chatId))

  private def publishLine(chatId: ChatId, line: Line, busChan: BusChan.Select): Funit =
    JsonView(line).map: json =>
      publish(chatId, ChatLine(chatId, line, json), busChan)

  def remove(chatId: ChatId) = coll.delete.one($id(chatId)).void

  def removeAll(chatIds: List[ChatId]) = coll.delete.one($inIds(chatIds)).void

  private def persistLine(chatId: ChatId, line: lila.core.chat.Line): Funit =
    import lila.chat.Line.given
    coll.update
      .one(
        $id(chatId),
        $doc(
          "$push" -> $doc(
            Chat.BSONFields.lines -> $doc(
              "$each"  -> List(line),
              "$slice" -> -150
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
