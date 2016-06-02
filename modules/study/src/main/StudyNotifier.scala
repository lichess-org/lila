package lila.study

import akka.actor._
import akka.pattern.ask

import lila.hub.actorApi.HasUserId
import lila.notify.InvitedToStudy.InvitedBy
import lila.notify.{ InvitedToStudy, NotifyApi, Notification }
import lila.relation.RelationApi
import makeTimeout.short
import org.joda.time.DateTime

private final class StudyNotifier(
    netBaseUrl: String,
    notifyApi: NotifyApi,
    relationApi: RelationApi) {

  def apply(study: Study, invited: lila.user.User, socket: ActorRef) =
    relationApi.fetchBlocks(invited.id, study.ownerId).flatMap {
      case true => funit
      case false =>
        socket ? HasUserId(invited.id) mapTo manifest[Boolean] map { isPresent =>
          study.owner.ifFalse(isPresent) foreach { owner =>
            val notificationContent = InvitedToStudy(InvitedToStudy.InvitedBy(owner.id), InvitedToStudy.StudyName(study.name), InvitedToStudy.StudyId(study.id))
            val notification = Notification(Notification.Notifies(invited.id), notificationContent)
            notifyApi.addNotification(notification)
          }
        }
    }

  private def studyUrl(study: Study) = s"$netBaseUrl/study/${study.id}"
}
