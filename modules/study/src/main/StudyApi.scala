package lila.study

import akka.actor.ActorRef

import chess.format.{ Forsyth, FEN }
import lila.hub.actorApi.map.Tell
import lila.hub.Sequencer
import lila.user.{ User, UserRepo }

final class StudyApi(
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo,
    sequencers: ActorRef,
    socketHub: akka.actor.ActorRef) {

  def byId = studyRepo byId _

  def byIdWithChapter(id: Study.ID): Fu[Option[Study.WithChapter]] = byId(id) flatMap {
    _ ?? { study =>
      chapterRepo.byId(study.position.chapterId) map {
        _ map { Study.WithChapter(study, _) }
      }
    }
  }

  def create(user: User): Fu[Study.WithChapter] = {
    val preStudy = Study.make(user = user.light)
    val chapter: Chapter = Chapter.make(
      studyId = preStudy.id,
      name = "Chapter 1",
      setup = Chapter.Setup(
        gameId = none,
        variant = chess.variant.Standard,
        orientation = chess.White),
      root = Node.Root.default,
      order = 1)
    val study = preStudy withChapter chapter
    studyRepo.insert(study) zip chapterRepo.insert(chapter) inject
      Study.WithChapter(study, chapter)
  }

  def setPath(userId: User.ID, studyId: Study.ID, position: Position.Ref) = sequenceStudy(studyId) { study =>
    Contribute(userId, study) {
      if (study.position.chapterId == position.chapterId) {
        (study.position.path != position.path) ?? {
          studyRepo.setPosition(study.id, position) >>-
            sendTo(study.id, Socket.SetPath(position.path))
        }
      }
      else {
        sendTo(study.id, Socket.ReloadUser(userId))
        funit
      }
    }
  }

  def addNode(studyId: Study.ID, position: Position.Ref, node: Node) = sequenceStudyWithChapter(studyId) {
    case Study.WithChapter(study, chapter) => Contribute(node.by, study) {
      chapter.addNode(position.path, node) match {
        case None =>
          sendTo(study.id, Socket.ReloadUser(node.by))
          funit
        case Some(newChapter) =>
          chapterRepo.update(newChapter) >>
            studyRepo.setPosition(study.id, position + node) >>-
            sendTo(study.id, Socket.AddNode(position, node))
      }
    }
  }

  def deleteNodeAt(userId: User.ID, studyId: Study.ID, position: Position.Ref) = ???
  // sequenceLocation(ref) { location =>
  // (location.study canWrite userId) ?? {
  //   val newChapter = location.chapter.updateRoot { root =>
  //     root.withChildren(_.deleteNodeAt(path))
  //   }
  //   studyRepo.setChapter(location withChapter newChapter) >>-
  //     sendTo(ref.studyId, Socket.DelNode(Position.Ref(ref.chapterId, path)))
  // }
  // }

  def promoteNodeAt(userId: User.ID, studyId: Study.ID, position: Position.Ref) = ???
  // sequenceLocation(ref) { location =>
  //   (location.study canWrite userId) ?? {
  //     val newChapter = location.chapter.updateRoot { root =>
  //       root.withChildren(_.promoteNodeAt(path))
  //     }
  //     studyRepo.setChapter(location withChapter newChapter)
  //   }
  // }

  def setRole(byUserId: User.ID, studyId: Study.ID, userId: User.ID, roleStr: String) = sequenceStudy(studyId) { study =>
    (study isOwner byUserId) ?? {
      val role = StudyMember.Role.byId.getOrElse(roleStr, StudyMember.Role.Read)
      studyRepo.setRole(study, userId, role) >>- reloadMembers(study)
    }
  }

  def invite(byUserId: User.ID, studyId: Study.ID, username: String) = sequenceStudy(studyId) { study =>
    (study isOwner byUserId) ?? {
      UserRepo.named(username).flatMap {
        _.filterNot(study.members.contains) ?? { user =>
          studyRepo.addMember(study, StudyMember.make(study, user))
        }
      } >>- reloadMembers(study)
    }
  }

  def kick(byUserId: User.ID, studyId: Study.ID, userId: User.ID) = sequenceStudy(studyId) { study =>
    study.members.contains(userId) ?? {
      studyRepo.removeMember(study, userId)
    } >>- reloadMembers(study)
  }

  def setShapes(userId: User.ID, studyId: Study.ID, shapes: List[Shape]) = sequenceStudy(studyId) { study =>
    Contribute(userId, study) {
      studyRepo.setShapes(study, shapes)
    } >>- reloadShapes(study)
  }

  private def reloadMembers(study: Study) =
    studyRepo.membersById(study.id).foreach {
      _ foreach { members =>
        sendTo(study.id, Socket.ReloadMembers(members))
      }
    }

  private def reloadShapes(study: Study) =
    studyRepo.getShapes(study.id).foreach { shapes =>
      sendTo(study.id, Socket.ReloadShapes(shapes))
    }

  private def sequenceStudy(studyId: String)(f: Study => Funit): Funit =
    byId(studyId) flatMap {
      _ ?? { study =>
        sequence(studyId)(f(study))
      }
    }

  private def sequenceStudyWithChapter(studyId: String)(f: Study.WithChapter => Funit): Funit =
    sequenceStudy(studyId) { study =>
      chapterRepo.byId(study.position.chapterId) flatMap {
        _ ?? { chapter =>
          f(Study.WithChapter(study, chapter))
        }
      }
    }

  private def sequence(studyId: String)(f: => Funit): Funit = {
    val promise = scala.concurrent.Promise[Unit]
    val work = Sequencer.work(f, promise.some)
    sequencers ! Tell(studyId, work)
    promise.future
  }

  import ornicar.scalalib.Zero
  private def Contribute[A](userId: User.ID, study: Study)(f: => A)(implicit default: Zero[A]): A =
    if (study canContribute userId) f else default.zero

  private def sendTo(studyId: String, msg: Any) {
    socketHub ! Tell(studyId, msg)
  }
}
