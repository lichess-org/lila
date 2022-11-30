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
    case Id.User(n)  => n into UserName
    case Id.Anon(id) => CuteNameGenerator fromSeed id.hashCode

object RacerPlayer:
  enum Id:
    case User(id: UserId)
    case Anon(sessionId: String)
  object Id:
    def apply(str: String) =
      if (str startsWith "@") Anon(str drop 1)
      else User(UserId(str))

  val lichess = Id.User(User.lichessId)

  def make(id: Id) = RacerPlayer(id = id, score = 0, createdAt = DateTime.now)
