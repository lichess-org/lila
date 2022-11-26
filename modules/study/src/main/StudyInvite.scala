package lila.study

import scala.concurrent.duration.*

import lila.db.dsl.{ *, given }
import lila.notify.{ InvitedToStudy, Notification, NotifyApi }
import lila.pref.Pref
import lila.relation.{ Block, Follow }
import lila.security.Granter
import lila.user.{ Holder, User }
import lila.study.BSONHandlers.given

final private class StudyInvite(
    studyRepo: StudyRepo,
    userRepo: lila.user.UserRepo,
    notifyApi: NotifyApi,
    prefApi: lila.pref.PrefApi,
    relationApi: lila.relation.RelationApi
)(using ec: scala.concurrent.ExecutionContext):

  private val notifyRateLimit = new lila.memo.RateLimit[User.ID](
    credits = 500,
    duration = 1 day,
    key = "study.invite.user"
  )

  private val maxMembers = 30

  def apply(
      byUserId: User.ID,
      study: Study,
      invitedUsername: String,
      getIsPresent: User.ID => Fu[Boolean]
  ): Fu[User] =
    for {
      _       <- (study.nbMembers >= maxMembers) ?? fufail[Unit](s"Max study members reached: $maxMembers")
      inviter <- userRepo named byUserId orFail "No such inviter"
      _ <- (!study.isOwner(inviter.id) && !Granter(_.StudyAdmin)(inviter)) ?? fufail[Unit](
        "Only the study owner can invite"
      )
      invited <-
        userRepo
          .named(invitedUsername)
          .map(
            _.filterNot(_.id == User.lichessId && !Granter(_.StudyAdmin)(inviter))
          ) orFail "No such invited"
      _         <- study.members.contains(invited) ?? fufail[Unit]("Already a member")
      relation  <- relationApi.fetchRelation(invited.id, byUserId)
      _         <- relation.has(Block) ?? fufail[Unit]("This user does not want to join")
      isPresent <- getIsPresent(invited.id)
      _ <-
        if (isPresent) funit
        else
          prefApi.getPref(invited).map(_.studyInvite).flatMap {
            case Pref.StudyInvite.ALWAYS => funit
            case Pref.StudyInvite.NEVER  => fufail("This user doesn't accept study invitations")
            case Pref.StudyInvite.FRIEND =>
              if (relation.has(Follow)) funit
              else fufail("This user only accept study invitations from friends")
          }
      _ <- studyRepo.addMember(study, StudyMember make invited)
      shouldNotify = !isPresent && (!inviter.marks.troll || relation.has(Follow))
      rateLimitCost =
        if (relation has Follow) 10
        else if (inviter.roles has "ROLE_COACH") 20
        else if (inviter.hasTitle) 20
        else if (inviter.perfs.bestRating >= 2000) 50
        else 100
      _ <- shouldNotify ?? notifyRateLimit(inviter.id, rateLimitCost) {
        val notificationContent = InvitedToStudy(
          UserId(inviter.id),
          study.name,
          study.id
        )
        val notification = Notification.make(UserId(invited.id), notificationContent)
        notifyApi.addNotification(notification).void
      }(funit)
    } yield invited

  def admin(study: Study, user: Holder): Funit =
    studyRepo.coll {
      _.update
        .one(
          $id(study.id),
          $set(s"members.${user.id}" -> $doc("role" -> "w", "admin" -> true)) ++
            $addToSet("uids"         -> user.id)
        )
    }.void
