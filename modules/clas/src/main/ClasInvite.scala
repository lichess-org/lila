package lila.clas

import reactivemongo.api.bson.Macros.Annotations.Key
import scalalib.ThreadLocalRandom

import lila.core.id.{ ClasId, ClasInviteId }
import lila.core.i18n.I18nKey.clas as trans
import lila.core.i18n.Translate

case class ClasInvite(
    @Key("_id") id: ClasInviteId, // random
    userId: UserId,
    realName: Student.RealName,
    clasId: ClasId,
    created: Clas.Recorded,
    accepted: Option[Boolean] = None
)

object ClasInvite:

  def make(clas: Clas, user: User, realName: Student.RealName)(using teacher: Me) =
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

    def flash(u: UserName)(using Translate): (String, String) = this match
      case Already => "success" -> trans.xisNowAStudentOfTheClass.txt(u)
      case Invited => "success" -> trans.anInvitationHasBeenSentToX.txt(u)
      case Found => "warning" -> trans.xAlreadyHasAPendingInvitation.txt(u)
      case CantMsgKid(url) => "warning" -> trans.xIsAKidAccountWarning.txt(u, url)
