package lila.study

import akka.actor.ActorRef
import org.joda.time.DateTime

import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.SendTo
import lila.hub.Sequencer

final class StudyApi(
    repo: StudyRepo,
    jsonView: JsonView,
    sequencers: ActorRef,
    socketHub: akka.actor.ActorRef) {

  def byId = repo byId _

  def locationByRef(ref: Location.Ref): Fu[Option[Location]] =
    byId(ref.studyId) map (_ flatMap (_ location ref.chapterId))

  def locationById(id: Location.Ref.ID): Fu[Option[Location]] =
    (Location.Ref parseId id) ?? locationByRef

  def setOwnerPath(refId: Location.Ref.ID, path: Path) = sequenceRef(refId) {
    repo.setOwnerPath(_, path)
  }

  def addStep(refId: Location.Ref.ID, step: Step) = sequenceRef(refId) {
    repo.setOwnerPath(_, path)
  }

  private def sequenceRef(refId: Location.Ref.ID)(f: Location.Ref => Funit): Funit =
    Location.Ref.parseId(refId) ?? { ref =>
      sequence(ref.studyId) {
        f(ref)
      }
    }

  private def sequence(studyId: String)(f: => Funit): Funit = {
    val promise = scala.concurrent.Promise[Unit]
    val work = Sequencer.work(f, promise.some)
    sequencers ! Tell(studyId, work)
    promise.future
  }
}
