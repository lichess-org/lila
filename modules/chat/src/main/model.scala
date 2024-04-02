package lila.chat

import play.api.libs.json.JsArray

import lila.core.shutup.PublicSource
import lila.user.User

case class UserModInfo(
    user: User,
    history: List[ChatTimeout.UserEntry]
)

enum BusChan:
  lazy val chan = s"chat:${toString.toLowerCase}"
  case Round
  case Tournament
  case Simul
  case Study
  case Team
  case Swiss
  case Global
object BusChan:
  type Select = BusChan.type => BusChan

case class GetLinkCheck(line: UserLine, source: PublicSource, promise: Promise[Boolean])
case class IsChatFresh(source: PublicSource, promise: Promise[Boolean])

opaque type JsonChatLines = JsArray
object JsonChatLines extends TotalWrapper[JsonChatLines, JsArray]:
  def empty: JsonChatLines = JsArray.empty
