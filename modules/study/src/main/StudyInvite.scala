package lila.study

import scala.concurrent.duration._

import lila.db.dsl._
import lila.notify.{ InvitedToStudy, Notification, NotifyApi }
import lila.pref.Pref
import lila.relation.{ Block, Follow }
import lila.user.User

final private class StudyInvite(
    studyRepo: StudyRepo,
    userRepo: lila.user.UserRepo,
    notifyApi: NotifyApi,
    prefApi: lila.pref.PrefApi,
    relationApi: lila.relation.RelationApi
)(implicit ec: scala.concurrent.ExecutionContext) {

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
      getIsPresent: User.ID => Fu[Boolean],
      role: StudyMember.Role = StudyMember.Role.Read
  ): Fu[User] =
    for {
      _ <- !(study.isOwner(byUserId) ||
        study.isPostGameStudyWithOpponentPlayer(byUserId)) ?? fufail[Unit](
        "Only study owner or study creators can invite"
      )
      _       <- (study.nbMembers >= maxMembers) ?? fufail[Unit](s"Max study members reached: $maxMembers")
      inviter <- userRepo.named(byUserId) orFail "No such inviter"
      invited <-
        userRepo
          .named(invitedUsername)
          .map(_.filterNot(_.id == User.lishogiId)) orFail "No such invited"
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
      _ <- studyRepo.addMember(study, StudyMember.make(invited, role))
      shouldNotify = !isPresent && (!inviter.marks.troll || relation.has(Follow))
      rateLimitCost =
        if (relation has Follow) 10
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
      }(funit)
    } yield invited

  def getInvitedUser(inviter: User, invitedUsername: String): Fu[User] =
    for {
      invited <-
        userRepo
          .named(invitedUsername)
          .map(_.filterNot(_.id == User.lishogiId)) orFail "No such user"
      _        <- (inviter.id == invited.id) ?? fufail[Unit]("You can't invite yourself")
      relation <- relationApi.fetchRelation(invited.id, inviter.id)
      _        <- relation.has(Block) ?? fufail[Unit]("This user does not want to join")
      _ <- prefApi.getPref(invited).map(_.studyInvite).flatMap {
        case Pref.StudyInvite.ALWAYS => funit
        case Pref.StudyInvite.NEVER  => fufail("This user doesn't accept study invitations")
        case Pref.StudyInvite.FRIEND =>
          if (relation.has(Follow)) funit
          else fufail("This user only accept study invitations from friends")
      }
    } yield invited

  def notify(study: Study, invitedId: User.ID, cost: Int): Funit =
    notifyRateLimit(study.ownerId, cost) {
      val notificationContent = InvitedToStudy(
        InvitedToStudy.InvitedBy(study.ownerId),
        InvitedToStudy.StudyName(study.name.value),
        InvitedToStudy.StudyId(study.id.value)
      )
      val notification = Notification.make(Notification.Notifies(invitedId), notificationContent)
      notifyApi.addNotification(notification)
    }(funit)

  def admin(study: Study, user: User): Funit =
    studyRepo.coll {
      _.update
        .one(
          $id(study.id.value),
          $set(s"members.${user.id}" -> $doc("role" -> "w", "admin" -> true)) ++
            $addToSet("uids"         -> user.id)
        )
    }.void
}
