package lila.chat

import play.api.libs.json.*

import lila.common.LightUser
import lila.common.Json.given
import lila.user.FlairApi

object JsonView:

  import writers.{ *, given }

  lazy val timeoutReasons = Json toJson ChatTimeout.Reason.all

  def asyncLines(chat: AnyChat)(using flairs: FlairApi)(using Executor): Fu[JsonChatLines] =
    flairs.flairsOf(chat.flairUserIds) map: flairs =>
      syncLines(chat)(using flairs)

  def syncLines(chat: AnyChat)(using FlairApi.FlairMap): JsonChatLines = JsonChatLines:
    chat match
      case c: MixedChat => JsArray(c.lines map lineWriter.writes)
      case c: UserChat  => JsArray(c.lines map userLineWriter.writes)

  def apply(line: Line)(using getFlair: FlairApi.Getter)(using Executor): Fu[JsObject] =
    line.userIdMaybe.ifTrue(line.flair).so(getFlair) map: flair =>
      given FlairApi.FlairMap = ~(for
        userId <- line.userIdMaybe
        flair  <- flair
      yield Map(userId -> flair))
      lineWriter.writes(line)

  def userModInfo(using LightUser.GetterSync)(u: UserModInfo) =
    lila.user.JsonView.modWrites.writes(u.user) ++ Json.obj:
      "history" -> u.history

  def mobile(chat: AnyChat, writeable: Boolean = true)(using FlairApi, Executor): Fu[JsObject] =
    asyncLines(chat).map: lines =>
      Json.obj(
        "lines"     -> lines,
        "writeable" -> writeable
      )

  def boardApi(chat: UserChat) = JsArray:
    chat.lines.collect:
      case UserLine(name, _, _, _, text, troll, del) if !troll && !del =>
        Json.obj("text" -> text, "user" -> name)

  object writers:

    given OWrites[ChatTimeout.Reason] = OWrites[ChatTimeout.Reason]: r =>
      Json.obj("key" -> r.key, "name" -> r.name)

    given (using lightUser: LightUser.GetterSync): OWrites[ChatTimeout.UserEntry] =
      OWrites[ChatTimeout.UserEntry]: e =>
        Json.obj(
          "reason" -> e.reason.key,
          "mod"    -> lightUser(e.mod).fold(UserName("?"))(_.name),
          "date"   -> e.createdAt
        )

    private[chat] def lineWriter(using FlairApi.FlairMap): OWrites[Line] = OWrites:
      case l: UserLine   => userLineWriter writes l
      case l: PlayerLine => playerLineWriter writes l

    def userLineWriter(using getFlair: FlairApi.FlairMap): OWrites[UserLine] = OWrites: l =>
      Json
        .obj(
          "u" -> l.username,
          "t" -> l.text
        )
        .add("r" -> l.troll)
        .add("d" -> l.deleted)
        .add("p" -> l.patron)
        .add("f" -> l.flair.so(getFlair.get(l.userId)))
        .add("title" -> l.title)

    val playerLineWriter: OWrites[PlayerLine] = OWrites: l =>
      Json.obj(
        "c" -> l.color.name,
        "t" -> l.text
      )
