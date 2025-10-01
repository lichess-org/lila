package lila.chat

import play.api.libs.json.*

import lila.common.Json.given
import lila.core.LightUser
import lila.core.user.LightUserApi

final class ChatJsonView(lightUser: LightUserApi)(using Executor):

  import ChatJsonView.*

  given OWrites[ChatTimeout.UserEntry] =
    OWrites[ChatTimeout.UserEntry]: e =>
      Json.obj(
        "reason" -> e.reason.key,
        "mod" -> lightUser.syncFallback(e.mod).name,
        "date" -> e.createdAt
      )

  def asyncLines(chat: AnyChat): Fu[JsonChatLines] =
    for users <- lightUser.asyncIdMapFallback(chat.userIds)
    yield syncLines(chat)(using users)

  def syncLines(chat: AnyChat)(using LightUser.IdMap): JsonChatLines = JsonChatLines:
    chat match
      case c: MixedChat => JsArray(c.lines.map(lineWriter.writes))
      case c: UserChat => JsArray(c.lines.map(userLineWriter.writes))

  private[chat] def asyncLine(line: Line): Fu[JsObject] =
    for users <- lightUser.asyncIdMapFallback(line.userIdMaybe.toSet)
    yield lineWriter(using users).writes(line)

  def userModInfo(u: UserModInfo) = modView(u.user) ++ Json.obj("history" -> u.history)

  private def modView(u: User) =
    Json.toJsObject(u.light) ++ Json
      .obj("games" -> u.count.game)
      .add("tos" -> u.marks.dirty)

  def mobile(chat: AnyChat, writeable: Boolean = true): Fu[JsObject] =
    asyncLines(chat).map: lines =>
      Json.obj(
        "lines" -> lines,
        "writeable" -> writeable
      )

  def boardApi(chat: UserChat) = JsArray:
    chat.lines.collect:
      case UserLine(name, text, troll, del) if !troll && !del =>
        Json.obj("text" -> text, "user" -> name)

object ChatJsonView:

  given OWrites[ChatTimeout.Reason] = OWrites[ChatTimeout.Reason]: r =>
    Json.obj("key" -> r.key, "name" -> r.name)

  private[chat] def lineWriter(using LightUser.IdMap): OWrites[Line] = OWrites:
    case l: UserLine => userLineWriter.writes(l)
    case l: PlayerLine => playerLineWriter.writes(l)

  def userLineWriter(using getUser: LightUser.IdMap): OWrites[UserLine] = OWrites: l =>
    val u = getUser.getOrElse(l.userId, LightUser.fallback(l.username))
    Json
      .obj(
        "u" -> u.name,
        "t" -> l.text
      )
      .add("r" -> l.troll)
      .add("d" -> l.deleted)
      .add("pc" -> u.patronAndColor.map(_.color))
      .add("f" -> u.flair)
      .add("title" -> u.title)

  val playerLineWriter: OWrites[PlayerLine] = OWrites: l =>
    Json.obj(
      "c" -> l.color.name,
      "t" -> l.text
    )

  val timeoutReasons = Json.toJson(ChatTimeout.Reason.all)
