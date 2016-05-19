package lila.study

import akka.actor.{ ActorRef, ActorSelection }
import org.apache.commons.lang3.StringEscapeUtils.escapeHtml4

import chess.format.pgn.{ Glyphs, Glyph }
import chess.format.{ Forsyth, FEN }
import lila.chat.actorApi.SystemTalk
import lila.hub.actorApi.map.Tell
import lila.hub.Sequencer
import lila.socket.Socket.Uid
import lila.socket.tree.Node.{ Shape, Comment }
import lila.user.{ User, UserRepo }

final class StudyApi(
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo,
    sequencers: ActorRef,
    chapterMaker: ChapterMaker,
    notifier: StudyNotifier,
    lightUser: lila.common.LightUser.Getter,
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

  def byIdWithChapter(id: Study.ID, chapterId: Chapter.ID): Fu[Option[Study.WithChapter]] = byId(id) flatMap {
    _ ?? { study =>
      chapterRepo.byId(chapterId) map {
        _ map { Study.WithChapter(study, _) }
      }
    }
  }

  def create(user: User): Fu[Study.WithChapter] = {
    val preStudy = Study.make(user)
    val chapter: Chapter = Chapter.make(
      studyId = preStudy.id,
      name = "Chapter 1",
      setup = Chapter.Setup(
        gameId = none,
        variant = chess.variant.Standard,
        orientation = chess.White),
      root = Node.Root.default(chess.variant.Standard),
      order = 1,
      ownerId = user.id,
      conceal = None)
    val study = preStudy withChapter chapter
    studyRepo.insert(study) zip chapterRepo.insert(chapter) inject
      Study.WithChapter(study, chapter)
  }

  def talk(userId: User.ID, studyId: Study.ID, text: String, socket: ActorRef) = byId(studyId) foreach {
    _ foreach { study =>
      (study.members contains userId) ?? {
        chat ! lila.chat.actorApi.UserTalk(studyId, userId, text, socket)
      }
    }
  }

  def setPath(userId: User.ID, studyId: Study.ID, position: Position.Ref, uid: Uid) = sequenceStudy(studyId) { study =>
    Contribute(userId, study) {
      chapterRepo.byId(position.chapterId).map {
        _ filter { c =>
          c.root.pathExists(position.path) && study.position.chapterId == c.id
        }
      } flatMap {
        case None => funit >>- reloadUid(study, uid)
        case Some(chapter) if study.position.path != position.path =>
          studyRepo.setPosition(study.id, position) >>
            updateConceal(study, chapter, position) >>-
            sendTo(study, Socket.SetPath(position, uid))
        case _ => funit
      }
    }
  }

  def addNode(userId: User.ID, studyId: Study.ID, position: Position.Ref, node: Node, uid: Uid) = sequenceStudyWithChapter(studyId) {
    case Study.WithChapter(study, chapter) => Contribute(userId, study) {
      chapter.addNode(node, position.path) match {
        case None => fufail(s"Invalid addNode $studyId $position $node") >>- reloadUid(study, uid)
        case Some(newChapter) =>
          chapterRepo.update(newChapter) >>
            studyRepo.setPosition(study.id, position + node) >>
            updateConceal(study, newChapter, position + node) >>-
            sendTo(study, Socket.AddNode(position, node, uid))
      }
    }
  }

  private def updateConceal(study: Study, chapter: Chapter, position: Position.Ref) =
    chapter.conceal ?? { conceal =>
      chapter.root.lastMainlinePlyOf(position.path).some.filter(_ > conceal) ?? { newConceal =>
        if (newConceal >= chapter.root.lastMainlinePly)
          chapterRepo.removeConceal(chapter.id) >>-
            sendTo(study, Socket.SetConceal(position, none))
        else
          chapterRepo.setConceal(chapter.id, newConceal) >>-
            sendTo(study, Socket.SetConceal(position, newConceal.some))
      }
    }

  def deleteNodeAt(userId: User.ID, studyId: Study.ID, position: Position.Ref, uid: Uid) = sequenceStudyWithChapter(studyId) {
    case Study.WithChapter(study, chapter) => Contribute(userId, study) {
      chapter.updateRoot { root =>
        root.withChildren(_.deleteNodeAt(position.path))
      } match {
        case Some(newChapter) =>
          chapterRepo.update(newChapter) >>-
            sendTo(study, Socket.DeleteNode(position, uid))
        case None => fufail(s"Invalid delNode $studyId $position") >>- reloadUid(study, uid)
      }
    }
  }

  def promoteNodeAt(userId: User.ID, studyId: Study.ID, position: Position.Ref, uid: Uid) = sequenceStudyWithChapter(studyId) {
    case Study.WithChapter(study, chapter) => Contribute(userId, study) {
      chapter.updateRoot { root =>
        root.withChildren(_.promoteNodeAt(position.path))
      } match {
        case Some(newChapter) =>
          chapterRepo.update(newChapter) >>-
            sendTo(study, Socket.PromoteNode(position, uid))
        case None => fufail(s"Invalid promoteNode $studyId $position") >>- reloadUid(study, uid)
      }
    }
  }

  def setRole(byUserId: User.ID, studyId: Study.ID, userId: User.ID, roleStr: String) = sequenceStudy(studyId) { study =>
    (study isOwner byUserId) ?? {
      val role = StudyMember.Role.byId.getOrElse(roleStr, StudyMember.Role.Read)
      studyRepo.setRole(study, userId, role) >>- reloadMembers(study)
    }
  }

  def invite(byUserId: User.ID, studyId: Study.ID, username: String, socket: ActorRef) = sequenceStudy(studyId) { study =>
    (study isOwner byUserId) ?? {
      UserRepo.named(username).flatMap {
        _.filterNot(study.members.contains) ?? { user =>
          studyRepo.addMember(study, StudyMember make user) >>-
            notifier(study, user, socket)
        }
      } >>- reloadMembers(study)
    }
  }

  def kick(byUserId: User.ID, studyId: Study.ID, userId: User.ID) = sequenceStudy(studyId) { study =>
    study.members.contains(userId) ?? {
      studyRepo.removeMember(study, userId)
    } >>- reloadMembers(study)
  }

  def setShapes(userId: User.ID, studyId: Study.ID, position: Position.Ref, shapes: List[Shape], uid: Uid) = sequenceStudy(studyId) { study =>
    Contribute(userId, study) {
      chapterRepo.byIdAndStudy(position.chapterId, study.id) flatMap {
        _ ?? { chapter =>
          chapter.setShapes(shapes, position.path) match {
            case Some(newChapter) =>
              chapterRepo.update(newChapter) >>-
                sendTo(study, Socket.SetShapes(position, shapes, uid))
            case None => fufail(s"Invalid setShapes $position $shapes") >>- reloadUid(study, uid)
          }
        }
      }
    }
  }

  def setComment(userId: User.ID, studyId: Study.ID, position: Position.Ref, text: Comment.Text, uid: Uid) = sequenceStudyWithChapter(studyId) {
    case Study.WithChapter(study, chapter) => Contribute(userId, study) {
      (study.members get userId) ?? { byMember =>
        lightUser(userId) ?? { author =>
          val comment = Comment(
            id = Comment.Id.make,
            text = text,
            by = Comment.Author.User(author.id, author.titleName))
          chapter.setComment(comment, position.path) match {
            case Some(newChapter) =>
              newChapter.root.nodeAt(position.path).flatMap(_.comments findBy comment.by) ?? { c =>
                chapterRepo.update(newChapter) >>-
                  sendTo(study, Socket.SetComment(position, c, uid))
              }
            case None => fufail(s"Invalid setComment $studyId $position") >>- reloadUid(study, uid)
          }
        }
      }
    }
  }

  def deleteComment(userId: User.ID, studyId: Study.ID, position: Position.Ref, id: Comment.Id, uid: Uid) = sequenceStudyWithChapter(studyId) {
    case Study.WithChapter(study, chapter) => Contribute(userId, study) {
      (study.members get userId) ?? { byMember =>
        chapter.deleteComment(id, position.path) match {
          case Some(newChapter) =>
            chapterRepo.update(newChapter) >>-
              sendTo(study, Socket.DeleteComment(position, id, uid))
          case None => fufail(s"Invalid deleteComment $studyId $position $id") >>- reloadUid(study, uid)
        }
      }
    }
  }

  def toggleGlyph(userId: User.ID, studyId: Study.ID, position: Position.Ref, glyph: Glyph, uid: Uid) = sequenceStudyWithChapter(studyId) {
    case Study.WithChapter(study, chapter) => Contribute(userId, study) {
      (study.members get userId) ?? { byMember =>
        chapter.toggleGlyph(glyph, position.path) match {
          case Some(newChapter) =>
            chapterRepo.update(newChapter) >>-
              newChapter.root.nodeAt(position.path).foreach { node =>
                sendTo(study, Socket.SetGlyphs(position, node.glyphs, uid))
              }
          case None => fufail(s"Invalid toggleGlyph $studyId $position $glyph") >>- reloadUid(study, uid)
        }
      }
    }
  }

  def addChapter(byUserId: User.ID, studyId: Study.ID, data: ChapterMaker.Data, socket: ActorRef, uid: Uid) = sequenceStudy(studyId) { study =>
    Contribute(byUserId, study) {
      chapterRepo.nextOrderByStudy(study.id) flatMap { order =>
        chapterMaker(study, data, order, byUserId) flatMap {
          _ ?? { chapter =>
            data.initial ?? {
              chapterRepo.firstByStudy(study.id) flatMap {
                _.filter(_.isEmptyInitial) ?? chapterRepo.delete
              }
            } >> chapterRepo.insert(chapter) >>
              doSetChapter(byUserId, study, chapter.id, socket, uid)
          }
        }
      }
    }
  }

  def setChapter(byUserId: User.ID, studyId: Study.ID, chapterId: Chapter.ID, socket: ActorRef, uid: Uid) = sequenceStudy(studyId) { study =>
    doSetChapter(byUserId, study, chapterId, socket, uid)
  }

  private def doSetChapter(byUserId: User.ID, study: Study, chapterId: Chapter.ID, socket: ActorRef, uid: Uid) =
    (study.canContribute(byUserId) && study.position.chapterId != chapterId) ?? {
      chapterRepo.byIdAndStudy(chapterId, study.id) flatMap {
        _ ?? { chapter =>
          studyRepo.updateSomeFields(study withChapter chapter) >>- {
            sendTo(study, Socket.ChangeChapter(uid))
            chat ! SystemTalk(study.id, escapeHtml4(chapter.name), socket)
          }
        }
      }
    }

  def editChapter(byUserId: User.ID, studyId: Study.ID, data: ChapterMaker.EditData, socket: ActorRef, uid: Uid) = sequenceStudy(studyId) { study =>
    Contribute(byUserId, study) {
      chapterRepo.byIdAndStudy(data.id, studyId) flatMap {
        _ ?? { chapter =>
          val name = Chapter toName data.name
          val newChapter = chapter.copy(
            name = name,
            conceal = (chapter.conceal, data.conceal) match {
              case (None, true)     => Chapter.Ply(0).some
              case (Some(_), false) => None
              case _                => chapter.conceal
            })
          if (chapter == newChapter) funit
          else chapterRepo.update(newChapter) >> {
            if (chapter.conceal != newChapter.conceal) {
              (newChapter.conceal.isDefined && study.position.chapterId == chapter.id).?? {
                val newPosition = study.position.withPath(Path.root)
                studyRepo.setPosition(study.id, newPosition)
              } >>-
                sendTo(study, Socket.ReloadAll)
            }
            else fuccess {
              reloadChapters(study)
              if (chapter.name != newChapter.name)
                chat ! SystemTalk(study.id, escapeHtml4(name), socket)
            }
          }
        }
      }
    }
  }

  def deleteChapter(byUserId: User.ID, studyId: Study.ID, chapterId: Chapter.ID, socket: ActorRef, uid: Uid) = sequenceStudy(studyId) { study =>
    Contribute(byUserId, study) {
      chapterRepo.byIdAndStudy(chapterId, studyId) flatMap {
        _ ?? { chapter =>
          chapterRepo.orderedMetadataByStudy(studyId).flatMap {
            case chaps if chaps.size > 1 => (study.position.chapterId == chapterId).?? {
              chaps.find(_.id != chapterId) ?? { newChap =>
                doSetChapter(byUserId, study, newChap.id, socket, uid)
              }
            } >> chapterRepo.delete(chapter.id)
            case _ => funit
          } >>- reloadChapters(study)
        }
      }
    }
  }

  def sortChapters(byUserId: User.ID, studyId: Study.ID, chapterIds: List[Chapter.ID], socket: ActorRef, uid: Uid) = sequenceStudy(studyId) { study =>
    Contribute(byUserId, study) {
      chapterRepo.sort(study, chapterIds) >>- reloadChapters(study)
    }
  }

  def editStudy(studyId: Study.ID, data: Study.Data) = sequenceStudy(studyId) { study =>
    data.settings ?? { settings =>
      val newStudy = study.copy(
        name = Study toName data.name,
        settings = settings,
        visibility = data.vis)
      (newStudy != study) ?? {
        studyRepo.updateSomeFields(newStudy) >>- sendTo(study, Socket.ReloadAll)
      }
    }
  }

  def delete(study: Study) = sequenceStudy(study.id) { study =>
    studyRepo.delete(study) >> chapterRepo.deleteByStudy(study)
  }

  private def reloadUid(study: Study, uid: Uid) =
    sendTo(study, Socket.ReloadUid(uid))

  private def reloadMembers(study: Study) =
    studyRepo.membersById(study.id).foreach {
      _ foreach { members =>
        sendTo(study, Socket.ReloadMembers(members))
      }
    }

  private def reloadChapters(study: Study) =
    chapterRepo.orderedMetadataByStudy(study.id).foreach { chapters =>
      sendTo(study, Socket.ReloadChapters(chapters))
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

  private def sendTo(study: Study, msg: Any) {
    socketHub ! Tell(study.id, msg)
  }
}
