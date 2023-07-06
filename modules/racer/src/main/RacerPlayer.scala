package lila.racer

import lila.common.CuteNameGenerator
import lila.user.User
import lila.common.LightUser

case class RacerPlayer(id: RacerPlayer.Id, user: Option[LightUser], createdAt: Instant, score: Int):

  import RacerPlayer.Id

  lazy val name: UserName = id match
    case Id.User(id) => user.fold(id into UserName)(_.name)
    case Id.Anon(id) => CuteNameGenerator fromSeed id.hashCode

object RacerPlayer:
  enum Id:
    case User(id: UserId)
    case Anon(sessionId: String)
  object Id:
    def apply(str: String) =
      if str startsWith "@" then Anon(str drop 1)
      else User(UserId(str))
    def userIdOf(id: Id) = id match
      case User(uid) => uid.some
      case _         => none

  val lichess = Id.User(User.lichessId)

  def make(id: Id, user: Option[LightUser]) =
    RacerPlayer(id = id, user = user, score = 0, createdAt = nowInstant)
