package lila.racer

import org.joda.time.DateTime
import lila.common.CuteNameGenerator
import lila.user.User

case class RacerPlayer(id: RacerPlayer.Id, createdAt: DateTime, score: Int):

  import RacerPlayer.Id

  lazy val userId: Option[UserId] = id match
    case Id.User(name) => name.id.some
    case _             => none

  lazy val name: UserName = id match
    case Id.User(n)  => n
    case Id.Anon(id) => CuteNameGenerator fromSeed id.hashCode

object RacerPlayer:
  enum Id:
    case User(name: UserName)
    case Anon(sessionId: String)
  object Id:
    def apply(str: String) =
      if (str startsWith "@") Anon(str drop 1)
      else User(UserName(str))

  val lichess = Id.User(User.lichessName)

  def make(id: Id) = RacerPlayer(id = id, score = 0, createdAt = DateTime.now)
