package lila.clas

import reactivemongo.api.bson.Macros.Annotations.Key
import scalalib.ThreadLocalRandom

import lila.core.id.{ ClasId, ClasInviteId }

case class ClasInvite(
    @Key("_id") id: ClasInviteId, // random
    userId: UserId,
    realName: String,
    clasId: ClasId,
    created: Clas.Recorded,
    accepted: Option[Boolean] = None
)

object ClasInvite:

  def make(clas: Clas, user: User, realName: String)(using teacher: Me) =
    ClasInvite(
      id = ClasInviteId(ThreadLocalRandom.nextString(8)),
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
