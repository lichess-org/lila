package lila.study

import akka.actor._
import akka.pattern.ask

import lila.hub.actorApi.HasUserId
import lila.notify.InvitedToStudy.InvitedBy
import lila.notify.{ InvitedToStudy, NotifyApi, Notification }
import lila.relation.RelationApi
import lila.user.{ User, UserRepo }
import makeTimeout.short
import org.joda.time.DateTime

private final class StudyNotifier(
    netBaseUrl: String,
    notifyApi: NotifyApi,
    relationApi: RelationApi) {

  def invite(study: Study, invited: User, socket: ActorRef) =
    canNotify(study.ownerId, invited) flatMap {
      _ ?? {
        socket ? HasUserId(invited.id) mapTo manifest[Boolean] map { isPresent =>
          study.owner.ifFalse(isPresent) foreach { owner =>
            val notificationContent = InvitedToStudy(InvitedToStudy.InvitedBy(owner.id), InvitedToStudy.StudyName(study.name), InvitedToStudy.StudyId(study.id))
            val notification = Notification.make(Notification.Notifies(invited.id), notificationContent)
            notifyApi.addNotification(notification)
          }
        }
      }
    }

  private def canNotify(fromId: User.ID, to: User): Fu[Boolean] =
    UserRepo.isTroll(fromId) flatMap {
      case true  => relationApi.fetchFollows(to.id, fromId)
      case false => !relationApi.fetchBlocks(to.id, fromId)
    }

  private def studyUrl(study: Study) = s"$netBaseUrl/study/${study.id}"
}
