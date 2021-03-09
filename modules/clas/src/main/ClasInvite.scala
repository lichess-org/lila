package lila.clas

import org.joda.time.DateTime

import lila.user.{ Holder, User }

case class ClasInvite(
    _id: ClasInvite.Id, // random
    userId: User.ID,
    realName: String,
    clasId: Clas.Id,
    created: Clas.Recorded,
    accepted: Option[Boolean] = None
)

object ClasInvite {

  case class Id(value: String) extends AnyVal with StringValue

  def make(clas: Clas, user: User, realName: String, teacher: Holder) =
    ClasInvite(
      _id = Id(lila.common.ThreadLocalRandom nextString 8),
      userId = user.id,
      realName = realName,
      clasId = clas.id,
      created = Clas.Recorded(by = teacher.id, at = DateTime.now)
    )

  sealed trait Feedback
  object Feedback {
    case object Already                extends Feedback
    case object Invited                extends Feedback
    case object Found                  extends Feedback
    case class CantMsgKid(url: String) extends Feedback
  }
}
