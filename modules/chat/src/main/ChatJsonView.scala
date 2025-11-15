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
    for users <- chatUsers(chat.userIds)
    yield JsonChatLines:
      chat match
        case c: MixedChat => JsArray(c.lines.map(lineWriter(users).writes))
        case c: UserChat => JsArray(c.lines.map(userLineWriter(users).writes))

  private[chat] def asyncLine(line: Line): Fu[JsObject] =
    for users <- chatUsers(line.userIdMaybe.toSet)
    yield lineWriter(users).writes(line)

  private def chatUsers(userIds: Set[UserId]): Fu[LightUser.IdMap] =
    lightUser.asyncIdMapFallback(userIds - UserId.lichess)

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

  private[chat] def lineWriter(users: LightUser.IdMap): OWrites[Line] = OWrites:
    case l: UserLine => userLineWriter(users).writes(l)
    case l: PlayerLine => playerLineWriter.writes(l)

  def userLineWriter(users: LightUser.IdMap): OWrites[UserLine] = OWrites: l =>
    val u = users.getOrElse(l.userId, LightUser.fallback(l.username))
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
