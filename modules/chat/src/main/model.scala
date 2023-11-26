package lila.chat

import lila.user.User
import lila.hub.actorApi.shutup.PublicSource
import play.api.libs.json.JsArray

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
