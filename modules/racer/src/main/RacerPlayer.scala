package lila.racer

import scalalib.cuteName.CuteNameGenerator

import lila.core.LightUser
import lila.user.User

case class RacerPlayer(id: RacerPlayer.Id, user: Option[LightUser], createdAt: Instant, score: Int):

  import RacerPlayer.Id

  lazy val name: UserName = id match
    case Id.User(id) => user.fold(id.into(UserName))(_.name)
    case Id.Anon(id) => UserName(CuteNameGenerator.fromSeed(id.hashCode))

object RacerPlayer:
  enum Id:
    case User(id: UserId)
    case Anon(sessionId: String)
  object Id:
    def apply(str: String) =
      if str.startsWith("@") then Anon(str.drop(1))
      else User(UserId(str))
    def userIdOf(id: Id) = id match
      case User(uid) => uid.some
      case _         => none

  val lichess = Id.User(User.lichessId)

  def make(id: Id, user: Option[LightUser]) =
    RacerPlayer(id = id, user = user, score = 0, createdAt = nowInstant)
