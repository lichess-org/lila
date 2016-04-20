package lila.study

import akka.actor.ActorRef

import chess.format.{ Forsyth, FEN }
import lila.hub.actorApi.map.Tell
import lila.hub.Sequencer
import lila.user.User

final class StudyApi(
    repo: StudyRepo,
    jsonView: JsonView,
    sequencers: ActorRef,
    socketHub: akka.actor.ActorRef) {

  def byId = repo byId _

  def create(user: User): Fu[Study] = {
    val study = Study.make(
      ownerId = user.id,
      setup = Chapter.Setup(
        gameId = none,
        variant = chess.variant.Standard,
        orientation = chess.White))
    repo insert study inject study
  }

  def locationByRef(ref: Location.Ref): Fu[Option[Location]] =
    byId(ref.studyId) map (_ flatMap (_ location ref.chapterId))

  def locationById(id: Location.Ref.ID): Fu[Option[Location]] =
    (Location.Ref parseId id) ?? locationByRef

  def setMemberPath(userId: User.ID, ref: Location.Ref, path: Path) =
    repo.setMemberPath(userId, ref, path)

  def addNode(ref: Location.Ref, path: Path, node: Node) = sequenceLocation(ref) { location =>
    val newChapter = location.chapter.updateRoot { root =>
      root.withChildren(_.addNodeAt(node, path))
    }
    repo.setChapter(location withChapter newChapter) >>
      repo.setMemberPath(node.by, ref, path + node)
  }

  def deleteNodeAt(ref: Location.Ref, path: Path) = sequenceLocation(ref) { location =>
    val newChapter = location.chapter.updateRoot { root =>
      root.withChildren(_.deleteNodeAt(path))
    }
    repo.setChapter(location withChapter newChapter.pp)
  }

  def promoteNodeAt(ref: Location.Ref, path: Path) = sequenceLocation(ref) { location =>
    val newChapter = location.chapter.updateRoot { root =>
      root.withChildren(_.promoteNodeAt(path))
    }
    repo.setChapter(location withChapter newChapter)
  }

  private def sequenceRef(refId: Location.Ref.ID)(f: Location.Ref => Funit): Funit =
    Location.Ref.parseId(refId) ?? { ref =>
      sequence(ref.studyId) {
        f(ref)
      }
    }

  private def sequenceLocation(ref: Location.Ref)(f: Location => Funit): Funit =
    locationByRef(ref) flatMap {
      _ ?? { location =>
        sequence(ref.studyId) {
          f(location)
        }
      }
    }

  private def sequence(studyId: String)(f: => Funit): Funit = {
    val promise = scala.concurrent.Promise[Unit]
    val work = Sequencer.work(f, promise.some)
    sequencers ! Tell(studyId, work)
    promise.future
  }
}
