package lila.study

import org.joda.time.DateTime

import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.SendTo

final class StudyApi(
    repo: StudyRepo,
    jsonView: JsonView,
    socketHub: akka.actor.ActorRef) {

  def byId = repo byId _
}
