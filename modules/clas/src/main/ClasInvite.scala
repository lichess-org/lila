package lila.clas

import ornicar.scalalib.ThreadLocalRandom

import lila.user.{ Me, User }

case class ClasInvite(
    _id: ClasInvite.Id, // random
    userId: UserId,
    realName: String,
    clasId: Clas.Id,
    created: Clas.Recorded,
    accepted: Option[Boolean] = None
)

object ClasInvite:

  opaque type Id = String
  object Id extends OpaqueString[Id]

  def make(clas: Clas, user: User, realName: String)(using teacher: Me) =
    ClasInvite(
      _id = Id(ThreadLocalRandom nextString 8),
      userId = user.id,
      realName = realName,
      clasId = clas.id,
      created = Clas.Recorded(by = teacher.userId, at = nowInstant)
    )

  enum Feedback:
    case Already
    case Invited
    case Found
    case CantMsgKid(url: String)
