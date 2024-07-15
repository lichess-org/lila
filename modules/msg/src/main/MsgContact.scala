package lila.msg

import reactivemongo.api.bson.*
import reactivemongo.api.bson.Macros.Annotations.Key

import lila.core.perm.{ Granter, Permission }
import lila.core.user.UserMarks
import lila.db.dsl.{ *, given }

private case class Contact(
    @Key("_id") id: UserId,
    kid: Option[Boolean],
    marks: Option[UserMarks],
    roles: Option[List[String]],
    createdAt: Instant
):
  def isKid                                = ~kid
  def isTroll                              = marks.exists(_.troll)
  def isVerified                           = roles.exists(_ contains "ROLE_VERIFIED")
  def isApiHog                             = roles.exists(_ contains "ROLE_API_HOG")
  def isDaysOld(days: Int)                 = createdAt.isBefore(nowInstant.minusDays(days))
  def isHoursOld(hours: Int)               = createdAt.isBefore(nowInstant.minusHours(hours))
  def isLichess                            = id.is(UserId.lichess)
  def isGranted(perm: Permission.Selector) = Granter.ofDbKeys(perm, ~roles)

private case class Contacts(orig: Contact, dest: Contact):
  def hasKid                     = orig.isKid || dest.isKid
  def userIds                    = List(orig.id, dest.id)
  def any(f: Contact => Boolean) = f(orig) || f(dest)

private final class ContactApi(userColl: Coll)(using Executor):

  private given BSONDocumentHandler[Contact] = Macros.handler[Contact]

  def contacts(orig: UserId, dest: UserId): Fu[Option[Contacts]] =
    import lila.core.user.{ BSONFields as F }
    userColl
      .byOrderedIds[Contact, UserId](
        List(orig, dest),
        $doc(F.kid -> true, F.marks -> true, F.roles -> true, F.createdAt -> true).some
      )(_.id)
      .map:
        case List(o, d) => Contacts(o, d).some
        case _          => none
