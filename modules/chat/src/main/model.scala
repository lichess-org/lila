package lila.chat

import play.api.libs.json.JsArray

import lila.core.chat.PublicSource

case class UserModInfo(
    user: User,
    history: List[ChatTimeout.UserEntry]
)

case class GetLinkCheck(line: UserLine, source: PublicSource, promise: Promise[Boolean])
case class IsChatFresh(source: PublicSource, promise: Promise[Boolean])

opaque type JsonChatLines = JsArray
object JsonChatLines extends TotalWrapper[JsonChatLines, JsArray]:
  def empty: JsonChatLines = JsArray.empty

private case class Speaker(
    username: UserName,
    title: Option[chess.PlayerTitle],
    flair: Option[lila.core.id.Flair],
    enabled: Boolean,
    plan: Option[lila.core.user.Plan],
    marks: Option[lila.core.user.UserMarks]
):
  def isBot = title.contains(chess.PlayerTitle.BOT)
  def isTroll = marks.exists(_.troll) || marks.exists(_.alt)
  def isPatron = plan.exists(_.active)
