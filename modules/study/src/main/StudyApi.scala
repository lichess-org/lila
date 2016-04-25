package lila.study

import akka.actor.{ ActorRef, ActorSelection }

import chess.format.{ Forsyth, FEN }
import lila.chat.actorApi.SystemTalk
import lila.hub.actorApi.map.Tell
import lila.hub.Sequencer
import lila.socket.Socket.Uid
import lila.user.{ User, UserRepo }

final class StudyApi(
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo,
    sequencers: ActorRef,
    chapterMaker: ChapterMaker,
    chat: ActorSelection,
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

  private def pathExists(position: Position.Ref): Fu[Boolean] =
    chapterRepo.byId(position.chapterId) map {
      _ ?? { _.root pathExists position.path }
    }

  def setPath(userId: User.ID, studyId: Study.ID, position: Position.Ref, uid: Uid) = sequenceStudy(studyId) { study =>
    Contribute(userId, study) {
      pathExists(position) flatMap { exists =>
        if (exists && study.position.chapterId == position.chapterId) {
          (study.position.path != position.path) ?? {
            studyRepo.setPosition(study.id, position) >>-
              sendTo(study.id, Socket.SetPath(position, uid))
          }
        }
        else funit >>- reloadUid(study, uid)
      }
    }
  }

  def addNode(studyId: Study.ID, position: Position.Ref, node: Node, uid: Uid) = sequenceStudyWithChapter(studyId) {
    case Study.WithChapter(study, chapter) => Contribute(node.by, study) {
      chapter.addNode(position.path, node) match {
        case None => fufail(s"Invalid addNode $position $node") >>- reloadUid(study, uid)
        case Some(newChapter) =>
          chapterRepo.update(newChapter) >>
            studyRepo.setPosition(study.id, position + node) >>-
            sendTo(study.id, Socket.AddNode(position, node, uid))
      }
    }
  }

  def deleteNodeAt(userId: User.ID, studyId: Study.ID, position: Position.Ref, uid: Uid) = ???
  // sequenceLocation(ref) { location =>
  // (location.study canWrite userId) ?? {
  //   val newChapter = location.chapter.updateRoot { root =>
  //     root.withChildren(_.deleteNodeAt(path))
  //   }
  //   studyRepo.setChapter(location withChapter newChapter) >>-
  //     sendTo(ref.studyId, Socket.DelNode(Position.Ref(ref.chapterId, path), uid))
  // }
  // }

  def promoteNodeAt(userId: User.ID, studyId: Study.ID, position: Position.Ref, uid: Uid) = ???
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

  def setShapes(userId: User.ID, studyId: Study.ID, shapes: List[Shape], uid: Uid) = sequenceStudy(studyId) { study =>
    Contribute(userId, study) {
      studyRepo.setShapes(study, shapes)
    } >>- reloadShapes(study, uid)
  }

  def addChapter(byUserId: User.ID, studyId: Study.ID, data: Chapter.FormData, socket: ActorRef) = sequenceStudy(studyId) { study =>
    (study isOwner byUserId) ?? {
      chapterRepo.nextOrderByStudy(study.id) flatMap { order =>
        chapterMaker(study, data, order) flatMap {
          _ ?? { chapter =>
            chapterRepo.insert(chapter) >>
              doSetChapter(byUserId, study, chapter.id, socket)
          }
        }
      }
    }
  }

  def setChapter(byUserId: User.ID, studyId: Study.ID, chapterId: Chapter.ID, socket: ActorRef) = sequenceStudy(studyId) { study =>
    doSetChapter(byUserId, study, chapterId, socket)
  }

  private def doSetChapter(byUserId: User.ID, study: Study, chapterId: Chapter.ID, socket: ActorRef) =
    (study.canContribute(byUserId) && study.position.chapterId != chapterId) ?? {
      chapterRepo.byIdAndStudy(chapterId, study.id) flatMap {
        _ ?? { chapter =>
          studyRepo.update(study withChapter chapter) >>- {
            reloadAll(study)
            study.members.get(byUserId).foreach { member =>
              import org.apache.commons.lang3.StringEscapeUtils.escapeHtml4
              val message = s"${member.user.name} switched to ${escapeHtml4(chapter.name)}"
              chat ! SystemTalk(study.id, message, socket)
            }
          }
        }
      }
    }

  def renameChapter(byUserId: User.ID, studyId: Study.ID, chapterId: Chapter.ID, name: String) = sequenceStudy(studyId) { study =>
    chapterRepo.byIdAndStudy(chapterId, studyId) flatMap {
      _ ?? { chapter =>
        chapterRepo.update(chapter.copy(name = name)) >>- reloadChapters(study)
      }
    }
  }

  def deleteChapter(byUserId: User.ID, studyId: Study.ID, chapterId: Chapter.ID, socket: ActorRef) = sequenceStudy(studyId) { study =>
    chapterRepo.byIdAndStudy(chapterId, studyId) flatMap {
      _ ?? { chapter =>
        chapterRepo.orderedMetadataByStudy(studyId).flatMap {
          case chaps if chaps.size > 1 => (study.position.chapterId == chapterId).?? {
            chaps.find(_.id != chapterId) ?? { newChap =>
              doSetChapter(byUserId, study, newChap.id, socket)
            }
          } >> chapterRepo.delete(chapter.id)
          case _ => funit
        } >>- reloadChapters(study)
      }
    }
  }

  private def reloadUid(study: Study, uid: Uid) =
    sendTo(study.id, Socket.ReloadUid(uid))

  private def reloadMembers(study: Study) =
    studyRepo.membersById(study.id).foreach {
      _ foreach { members =>
        sendTo(study.id, Socket.ReloadMembers(members))
      }
    }

  private def reloadChapters(study: Study) =
    chapterRepo.orderedMetadataByStudy(study.id).foreach { chapters =>
      sendTo(study.id, Socket.ReloadChapters(chapters))
    }

  private def reloadAll(study: Study) =
    chapterRepo.orderedMetadataByStudy(study.id).foreach { chapters =>
      sendTo(study.id, Socket.ReloadAll(study, chapters))
    }

  private def reloadShapes(study: Study, uid: Uid) =
    studyRepo.getShapes(study.id).foreach { shapes =>
      sendTo(study.id, Socket.ReloadShapes(shapes, uid))
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
