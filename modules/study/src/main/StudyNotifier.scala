package lila.study

import akka.actor._
import akka.pattern.ask

import lila.hub.actorApi.HasUserId
import lila.hub.actorApi.message.LichessThread
import makeTimeout.short

private final class StudyNotifier(
    messageActor: ActorSelection,
    netBaseUrl: String) {

  def apply(study: Study, invited: lila.user.User, socket: ActorRef) =
    socket ? HasUserId(invited.id) mapTo manifest[Boolean] map { isPresent =>
      study.owner.ifFalse(isPresent) foreach { owner =>
        if (!isPresent) messageActor ! LichessThread(
          from = owner.id,
          to = invited.id,
          subject = s"Would you like to join my study?",
          message = s"I invited you to this study: ${studyUrl(study)}",
          notification = true)
      }
    }

  private def studyUrl(study: Study) = s"$netBaseUrl/study/${study.id}"
}
