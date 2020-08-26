package lila.chat

import lila.user.User
import scala.concurrent.Promise
import lila.hub.actorApi.shutup.PublicSource

case class UserModInfo(
    user: User,
    history: List[ChatTimeout.UserEntry]
)

sealed trait BusChan {
  lazy val chan = s"chat:${toString.toLowerCase}"
}
object BusChan {
  case object Round      extends BusChan
  case object Tournament extends BusChan
  case object Simul      extends BusChan
  case object Study      extends BusChan
  case object Team       extends BusChan
  case object Swiss      extends BusChan
  case object Global     extends BusChan

  type Select = BusChan.type => BusChan
}

case class GetLinkCheck(line: UserLine, source: PublicSource, promise: Promise[Boolean])
