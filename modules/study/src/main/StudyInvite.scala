package lila.study

import akka.actor._
import akka.pattern.ask

import lila.hub.actorApi.HasUserId
import lila.notify.{ InvitedToStudy, NotifyApi, Notification }
import lila.relation.RelationApi
import lila.user.{ User, UserRepo }
import makeTimeout.short

private final class StudyInvite(
    notifyApi: NotifyApi,
    relationApi: RelationApi
) {

  private val maxMembers = 30

  def notify(byUserId: User.ID, study: Study, invitedUsername: String, socket: ActorRef): Funit =
    study.isOwner(byUserId) ?? {
      if (study.nbMembers >= maxMembers) fufail(s"Max study members reached: $maxMembers")
      else for {
        invited <- UserRepo.named(username) flatten "No such user"
        _ <- if (study.members contains invited) fufail("Already a member") else funit
        relation <- relationApi.fetchRelation(invited.id, byUserId)
        isPresent <- socket ? HasUserId(invited.id) mapTo manifest[Boolean]

    (study.isOwner(byUserId) && study.nbMembers < 30) ?? {
    canNotify(study.ownerId, invited) flatMap {
      _ ?? {
        socket ? HasUserId(invited.id) mapTo manifest[Boolean] map { isPresent =>
          study.owner.ifFalse(isPresent) foreach { owner =>
            val notificationContent = InvitedToStudy(
              InvitedToStudy.InvitedBy(owner.id),
              InvitedToStudy.StudyName(study.name.value),
              InvitedToStudy.StudyId(study.id.value)
            )
            val notification = Notification.make(Notification.Notifies(invited.id), notificationContent)
            notifyApi.addNotification(notification)
          }
        }
      }
    }

  private def canNotify(fromId: User.ID, to: User): Fu[Boolean] =
    UserRepo.isTroll(fromId) flatMap {
      case true => relationApi.fetchFollows(to.id, fromId)
      case false => !relationApi.fetchBlocks(to.id, fromId)
    }
}
