package lila.study

import akka.actor._
import akka.pattern.ask
import scala.concurrent.duration._

import lila.notify.{ InvitedToStudy, NotifyApi, Notification }
import lila.pref.Pref
import lila.relation.{ Block, Follow }
import lila.user.{ User, UserRepo }
import makeTimeout.short

private final class StudyInvite(
    studyRepo: StudyRepo,
    notifyApi: NotifyApi,
    getPref: User => Fu[Pref],
    getRelation: (User.ID, User.ID) => Fu[Option[lila.relation.Relation]]
) {

  private val notifyRateLimit = new lila.memo.RateLimit[User.ID](
    credits = 500,
    duration = 1 day,
    name = "Study invites per user",
    key = "study.invite.user"
  )

  private val maxMembers = 30

  def apply(byUserId: User.ID, study: Study, invitedUsername: String, getIsPresent: User.ID => Fu[Boolean]): Funit = for {
    _ <- !study.isOwner(byUserId) ?? fufail[Unit]("Only study owner can invite")
    _ <- (study.nbMembers >= maxMembers) ?? fufail[Unit](s"Max study members reached: $maxMembers")
    inviter <- UserRepo.named(byUserId) flatten "No such inviter"
    invited <- UserRepo.named(invitedUsername).map(_.filterNot(_.id == User.lichessId)) flatten "No such invited"
    _ <- study.members.contains(invited) ?? fufail[Unit]("Already a member")
    relation <- getRelation(invited.id, byUserId)
    _ <- relation.has(Block) ?? fufail[Unit]("This user does not want to join")
    isPresent <- getIsPresent(invited.id)
    _ <- if (isPresent) funit else getPref(invited).map(_.studyInvite).flatMap {
      case Pref.StudyInvite.ALWAYS => funit
      case Pref.StudyInvite.NEVER => fufail("This user doesn't accept study invitations")
      case Pref.StudyInvite.FRIEND =>
        if (relation.has(Follow)) funit
        else fufail("This user only accept study invitations from friends")
    }
    _ <- studyRepo.addMember(study, StudyMember make invited)
    shouldNotify = !isPresent && (!inviter.troll || relation.has(Follow))
    rateLimitCost = if (relation has Follow) 10
    else if (inviter.roles has "ROLE_COACH") 20
    else if (inviter.hasTitle) 20
    else if (inviter.perfs.bestRating >= 2000) 50
    else if (invited.hasTitle) 200
    else 100
    _ <- shouldNotify ?? notifyRateLimit(inviter.id, rateLimitCost) {
      val notificationContent = InvitedToStudy(
        InvitedToStudy.InvitedBy(inviter.id),
        InvitedToStudy.StudyName(study.name.value),
        InvitedToStudy.StudyId(study.id.value)
      )
      val notification = Notification.make(Notification.Notifies(invited.id), notificationContent)
      notifyApi.addNotification(notification)
    }
  } yield ()
}
