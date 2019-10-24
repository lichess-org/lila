package lila.study

import akka.actor.ActorSelection
import scala.concurrent.duration._

import actorApi.Who
import chess.Centis
import chess.format.pgn.{ Tags, Glyph }
import lila.chat.Chat
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.timeline.{ Propagate, StudyCreate, StudyLike }
import lila.socket.Socket.Sri
import lila.tree.Eval
import lila.tree.Node.{ Shapes, Comment, Gamebook }
import lila.user.User

final class StudyApi(
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo,
    sequencer: StudySequencer,
    studyMaker: StudyMaker,
    chapterMaker: ChapterMaker,
    inviter: StudyInvite,
    explorerGameHandler: ExplorerGame,
    lightUser: lila.common.LightUser.GetterSync,
    scheduler: akka.actor.Scheduler,
    chat: ActorSelection,
    bus: lila.common.Bus,
    timeline: ActorSelection,
    socketMap: SocketMap,
    serverEvalRequester: ServerEval.Requester,
    lightStudyCache: LightStudyCache
) {

  import sequencer._

  def byId = studyRepo byId _

  def byIds = studyRepo byOrderedIds _

  def publicIdNames = studyRepo publicIdNames _

  def publicByIds(ids: Seq[Study.Id]) = byIds(ids) map { _.filter(_.isPublic) }

  def byIdAndOwner(id: Study.Id, owner: User) = byId(id) map {
    _.filter(_ isOwner owner.id)
  }

  def isOwner(id: Study.Id, owner: User) = byIdAndOwner(id, owner).map(_.isDefined)

  def byIdWithChapter(id: Study.Id): Fu[Option[Study.WithChapter]] = byId(id) flatMap {
    _ ?? { study =>
      chapterRepo byId study.position.chapterId flatMap {
        case None => chapterRepo firstByStudy study.id flatMap {
          case None => fuccess(none)
          case Some(chapter) =>
            val fixed = study withChapter chapter
            studyRepo updateSomeFields fixed inject
              Study.WithChapter(fixed, chapter).some
        }
        case Some(chapter) => fuccess(Study.WithChapter(study, chapter).some)
      }
    }
  }

  def byIdWithChapter(id: Study.Id, chapterId: Chapter.Id): Fu[Option[Study.WithChapter]] = byId(id) flatMap {
    _ ?? { study =>
      chapterRepo byId chapterId map {
        _.filter(_.studyId == study.id) map { Study.WithChapter(study, _) }
      }
    }
  }

  def byIdWithFirstChapter(id: Study.Id): Fu[Option[Study.WithChapter]] = byId(id) flatMap {
    _ ?? { study =>
      chapterRepo.firstByStudy(study.id) map {
        _ ?? { Study.WithChapter(study, _).some }
      }
    }
  }

  def studyIdOf = chapterRepo.studyIdOf _

  def members(id: Study.Id): Fu[Option[StudyMembers]] = studyRepo membersById id

  def importGame(data: StudyMaker.ImportGame, user: User): Fu[Option[Study.WithChapter]] = (data.form.as match {
    case DataForm.importGame.AsNewStudy =>
      studyMaker(data, user) flatMap { res =>
        studyRepo.insert(res.study) >>
          chapterRepo.insert(res.chapter) >>-
          indexStudy(res.study) >>-
          scheduleTimeline(res.study.id) inject res.some
      }
    case DataForm.importGame.AsChapterOf(studyId) => byId(studyId) flatMap {
      case Some(study) if study.canContribute(user.id) =>
        import akka.pattern.ask
        import makeTimeout.short
        addChapter(
          studyId = study.id,
          data = data.form.toChapterData,
          sticky = study.settings.sticky
        )(Who(user.id, Sri(""))) >> byIdWithChapter(studyId)
      case _ => fuccess(none)
    } orElse importGame(data.copy(form = data.form.copy(asStr = none)), user)
  }) addEffect {
    _ ?? { sc =>
      bus.publish(actorApi.StartStudy(sc.study.id), 'startStudy)
    }
  }

  def clone(me: User, prev: Study): Fu[Option[Study]] =
    Settings.UserSelection.allows(prev.settings.cloneable, prev, me.id.some) ?? {
      chapterRepo.orderedByStudy(prev.id).flatMap { chapters =>
        val study1 = prev.cloneFor(me)
        val newChapters = chapters.map(_ cloneFor study1)
        newChapters.headOption.map(study1.rewindTo) ?? { study =>
          studyRepo.insert(study) >>
            newChapters.map(chapterRepo.insert).sequenceFu >>- {
              chat ! lila.chat.actorApi.SystemTalk(
                Chat.Id(study.id.value),
                s"Cloned from lichess.org/study/${prev.id}"
              )
            } inject study.some
        }
      }
    }

  def resetIfOld(study: Study, chapters: List[Chapter.Metadata]): Fu[(Study, Option[Chapter])] =
    chapters.headOption match {
      case Some(c) if study.isOld && study.position != c.initialPosition =>
        val newStudy = study rewindTo c
        studyRepo.updateSomeFields(newStudy) zip chapterRepo.byId(c.id) map {
          case (_, chapter) => newStudy -> chapter
        }
      case _ => fuccess(study -> none)
    }

  private def scheduleTimeline(studyId: Study.Id) = scheduler.scheduleOnce(1 minute) {
    byId(studyId) foreach {
      _.filter(_.isPublic) foreach { study =>
        timeline ! (Propagate(StudyCreate(study.ownerId, study.id.value, study.name.value)) toFollowersOf study.ownerId)
      }
    }
  }

  def talk(userId: User.ID, studyId: Study.Id, text: String) = byId(studyId) foreach {
    _ foreach { study =>
      (study canChat userId) ?? {
        chat ! lila.chat.actorApi.UserTalk(
          Chat.Id(studyId.value),
          userId = userId,
          text = text,
          publicSource = lila.hub.actorApi.shutup.PublicSource.Study(studyId.value).some
        )
      }
    }
  }

  def setPath(studyId: Study.Id, position: Position.Ref)(who: Who): Funit = sequenceStudy(studyId) { study =>
    Contribute(who.u, study) {
      chapterRepo.byId(position.chapterId).map {
        _ filter { c =>
          c.root.pathExists(position.path) && study.position.chapterId == c.id
        }
      } flatMap {
        case None => funit >>- reloadSri(study, who.sri)
        case Some(chapter) if study.position.path != position.path =>
          studyRepo.setPosition(study.id, position) >>
            updateConceal(study, chapter, position) >>-
            sendToNew(study.id, _.SetPath(position, who))
        case _ => funit
      }
    }
  }

  def addNode(studyId: Study.Id, position: Position.Ref, node: Node, opts: MoveOpts, relay: Option[Chapter.Relay] = None)(who: Who) =
    sequenceStudyWithChapter(studyId, position.chapterId) {
      case Study.WithChapter(study, chapter) => Contribute(who.u, study) {
        doAddNode(study, Position(chapter, position.path), node, opts, relay)(who).void
      }
    }

  private def doAddNode(study: Study, position: Position, rawNode: Node, opts: MoveOpts, relay: Option[Chapter.Relay])(who: Who): Funit = {
    val node = rawNode.withoutChildren
    def failReload = reloadSriBecauseOf(study, who.sri, position.chapter.id)
    if (position.chapter.isOverweight) {
      logger.info(s"Overweight chapter ${study.id}/${position.chapter.id}")
      fuccess(failReload)
    } else position.chapter.addNode(node, position.path, relay) match {
      case None =>
        failReload
        fufail(s"Invalid addNode ${study.id} ${position.ref} $node")
      case Some(chapter) =>
        chapter.root.nodeAt(position.path) ?? { parent =>
          val newPosition = position.ref + node
          chapterRepo.setChildren(parent.children)(chapter, position.path) >>
            (relay ?? { chapterRepo.setRelay(chapter.id, _) }) >>
            (opts.sticky ?? studyRepo.setPosition(study.id, newPosition)) >>
            updateConceal(study, chapter, newPosition) >>- {
              sendTo(study, StudySocket.AddNode(
                position.ref,
                node,
                chapter.setup.variant,
                who.sri,
                sticky = opts.sticky,
                relay = relay
              ))
              sendStudyEnters(study, who.u)
              if (opts.promoteToMainline && !Path.isMainline(chapter.root, newPosition.path))
                promote(study.id, position.ref + node, toMainline = true)(who)
            }
        }
    }
  }

  private def updateConceal(study: Study, chapter: Chapter, position: Position.Ref) =
    chapter.conceal ?? { conceal =>
      chapter.root.lastMainlinePlyOf(position.path).some.filter(_ > conceal) ?? { newConceal =>
        if (newConceal >= chapter.root.lastMainlinePly)
          chapterRepo.removeConceal(chapter.id) >>-
            sendTo(study, StudySocket.SetConceal(position, none))
        else
          chapterRepo.setConceal(chapter.id, newConceal) >>-
            sendTo(study, StudySocket.SetConceal(position, newConceal.some))
      }
    }

  def deleteNodeAt(studyId: Study.Id, position: Position.Ref)(who: Who) = sequenceStudyWithChapter(studyId, position.chapterId) {
    case Study.WithChapter(study, chapter) => Contribute(who.u, study) {
      chapter.updateRoot { root =>
        root.withChildren(_.deleteNodeAt(position.path))
      } match {
        case Some(newChapter) =>
          chapterRepo.update(newChapter) >>-
            sendTo(study, StudySocket.DeleteNode(position, who.sri))
        case None =>
          fufail(s"Invalid delNode $studyId $position") >>-
            reloadSriBecauseOf(study, who.sri, chapter.id)
      }
    }
  }

  def clearAnnotations(studyId: Study.Id, chapterId: Chapter.Id)(who: Who) = sequenceStudyWithChapter(studyId, chapterId) {
    case Study.WithChapter(study, chapter) => Contribute(who.u, study) {
      chapterRepo.update(chapter.updateRoot { root =>
        root.withChildren(_.updateAllWith(_.clearAnnotations).some)
      } | chapter) >>- sendTo(study, StudySocket.UpdateChapter(who.sri, chapter.id))
    }
  }

  def promote(studyId: Study.Id, position: Position.Ref, toMainline: Boolean)(who: Who) = sequenceStudyWithChapter(studyId, position.chapterId) {
    case Study.WithChapter(study, chapter) => Contribute(who.u, study) {
      chapter.updateRoot { root =>
        root.withChildren { children =>
          if (toMainline) children.promoteToMainlineAt(position.path)
          else children.promoteUpAt(position.path).map(_._1)
        }
      } match {
        case Some(newChapter) =>
          chapterRepo.update(newChapter) >>-
            sendTo(study, StudySocket.Promote(position, toMainline, who.sri)) >>
            newChapter.root.children.nodesOn {
              newChapter.root.mainlinePath.intersect(position.path)
            }.collect {
              case (node, path) if node.forceVariation =>
                doForceVariation(Study.WithChapter(study, newChapter), path, false, Sri(""))
            }.sequenceFu.void
        case None =>
          fufail(s"Invalid promoteToMainline $studyId $position") >>-
            reloadSriBecauseOf(study, who.sri, chapter.id)
      }
    }
  }

  def forceVariation(studyId: Study.Id, position: Position.Ref, force: Boolean)(who: Who): Funit =
    sequenceStudyWithChapter(studyId, position.chapterId) { sc =>
      Contribute(who.u, sc.study) {
        doForceVariation(sc, position.path, force, who.sri)
      }
    }

  private def doForceVariation(sc: Study.WithChapter, path: Path, force: Boolean, sri: Sri): Funit =
    sc.chapter.forceVariation(force, path) match {
      case Some(newChapter) =>
        chapterRepo.forceVariation(force)(newChapter, path) >>-
          sendTo(sc.study, StudySocket.ForceVariation(Position(newChapter, path).ref, force, sri))
      case None =>
        fufail(s"Invalid forceVariation ${Position(sc.chapter, path)} $force") >>-
          reloadSriBecauseOf(sc.study, sri, sc.chapter.id)
    }

  def setRole(studyId: Study.Id, userId: User.ID, roleStr: String)(who: Who) = sequenceStudy(studyId) { study =>
    (study isOwner who.u) ?? {
      val role = StudyMember.Role.byId.getOrElse(roleStr, StudyMember.Role.Read)
      study.members.get(userId) ifTrue study.isPublic foreach { member =>
        if (!member.role.canWrite && role.canWrite)
          bus.publish(lila.hub.actorApi.study.StudyMemberGotWriteAccess(userId, studyId.value), 'study)
        else if (member.role.canWrite && !role.canWrite)
          bus.publish(lila.hub.actorApi.study.StudyMemberLostWriteAccess(userId, studyId.value), 'study)
      }
      studyRepo.setRole(study, userId, role) >>-
        onMembersChange(study)
    }
  }

  def invite(byUserId: User.ID, studyId: Study.Id, username: String, socket: StudySocket, onError: String => Unit) = sequenceStudy(studyId) { study =>
    inviter(byUserId, study, username, socket).addEffects(
      err => onError(err.getMessage),
      _ => onMembersChange(study)
    )
  }

  def kick(studyId: Study.Id, userId: User.ID)(who: Who) = sequenceStudy(studyId) { study =>
    (study.isMember(userId) && (study.isOwner(who.u) ^ (who.u == userId))) ?? {
      if (study.isPublic && study.canContribute(userId))
        bus.publish(lila.hub.actorApi.study.StudyMemberLostWriteAccess(userId, studyId.value), 'study)
      studyRepo.removeMember(study, userId)
    } >>- onMembersChange(study)
  }

  def isContributor = studyRepo.isContributor _
  def isMember = studyRepo.isMember _

  private def onMembersChange(study: Study) = {
    lightStudyCache.refresh(study.id)
    studyRepo.membersById(study.id).foreach {
      _ foreach { members =>
        sendTo(study, StudySocket.ReloadMembers(members))
      }
    }
    indexStudy(study)
  }

  def setShapes(studyId: Study.Id, position: Position.Ref, shapes: Shapes)(who: Who) = sequenceStudy(studyId) { study =>
    Contribute(who.u, study) {
      chapterRepo.byIdAndStudy(position.chapterId, study.id) flatMap {
        _ ?? { chapter =>
          chapter.setShapes(shapes, position.path) match {
            case Some(newChapter) =>
              studyRepo.updateNow(study)
              chapterRepo.setShapes(shapes)(newChapter, position.path) >>-
                sendTo(study, StudySocket.SetShapes(position, shapes, who.sri))
            case None =>
              fufail(s"Invalid setShapes $position $shapes") >>-
                reloadSriBecauseOf(study, who.sri, chapter.id)
          }
        }
      }
    }
  }

  def setClock(studyId: Study.Id, position: Position.Ref, clock: Option[Centis], sri: Sri): Funit =
    sequenceStudyWithChapter(studyId, position.chapterId) { sc =>
      sc.chapter.setClock(clock, position.path) match {
        case Some(newChapter) =>
          studyRepo.updateNow(sc.study)
          chapterRepo.setClock(clock)(newChapter, position.path) >>-
            sendTo(sc.study, StudySocket.SetClock(position, clock, sri))
        case None =>
          fufail(s"Invalid setClock $position $clock") >>-
            reloadSriBecauseOf(sc.study, sri, position.chapterId)
      }
    }

  def setTag(studyId: Study.Id, setTag: actorApi.SetTag)(who: Who) = sequenceStudyWithChapter(studyId, setTag.chapterId) {
    case Study.WithChapter(study, chapter) => Contribute(who.u, study) {
      doSetTags(study, chapter, PgnTags(chapter.tags + setTag.tag), who.sri)
    }
  }

  def setTags(studyId: Study.Id, chapterId: Chapter.Id, tags: Tags, sri: Sri)(who: Who) = sequenceStudyWithChapter(studyId, chapterId) {
    case Study.WithChapter(study, chapter) => Contribute(who.u, study) {
      doSetTags(study, chapter, tags, who.sri)
    }
  }

  private def doSetTags(study: Study, oldChapter: Chapter, tags: Tags, sri: Sri): Funit = {
    val chapter = oldChapter.copy(tags = tags)
    (chapter.tags != oldChapter.tags) ?? {
      chapterRepo.setTagsFor(chapter) >> {
        PgnTags.setRootClockFromTags(chapter) ?? { c =>
          setClock(study.id, Position(c, Path.root).ref, c.root.clock, sri)
        }
      } >>-
        sendTo(study, StudySocket.SetTags(chapter.id, chapter.tags, sri))
    } >>- indexStudy(study)
  }

  def setComment(studyId: Study.Id, position: Position.Ref, text: Comment.Text)(who: Who) = sequenceStudyWithChapter(studyId, position.chapterId) {
    case Study.WithChapter(study, chapter) => Contribute(who.u, study) {
      lightUser(who.u) ?? { author =>
        val comment = Comment(
          id = Comment.Id.make,
          text = text,
          by = Comment.Author.User(author.id, author.titleName)
        )
        doSetComment(who.u, study, Position(chapter, position.path), comment, who.sri)
      }
    }
  }

  private def doSetComment(userId: User.ID, study: Study, position: Position, comment: Comment, sri: Sri): Funit =
    position.chapter.setComment(comment, position.path) match {
      case Some(newChapter) =>
        studyRepo.updateNow(study)
        newChapter.root.nodeAt(position.path) ?? { node =>
          node.comments.findBy(comment.by) ?? { c =>
            chapterRepo.setComments(node.comments.filterEmpty)(newChapter, position.path) >>- {
              sendTo(study, StudySocket.SetComment(position.ref, c, sri))
              indexStudy(study)
              sendStudyEnters(study, userId)
            }
          }
        }
      case None =>
        fufail(s"Invalid setComment ${study.id} $position") >>-
          reloadSriBecauseOf(study, sri, position.chapter.id)
    }

  def deleteComment(studyId: Study.Id, position: Position.Ref, id: Comment.Id)(who: Who) = sequenceStudyWithChapter(studyId, position.chapterId) {
    case Study.WithChapter(study, chapter) => Contribute(who.u, study) {
      chapter.deleteComment(id, position.path) match {
        case Some(newChapter) =>
          chapterRepo.update(newChapter) >>-
            sendTo(study, StudySocket.DeleteComment(position, id, who.sri)) >>-
            indexStudy(study)
        case None =>
          fufail(s"Invalid deleteComment $studyId $position $id") >>-
            reloadSriBecauseOf(study, who.sri, chapter.id)
      }
    }
  }

  def toggleGlyph(studyId: Study.Id, position: Position.Ref, glyph: Glyph)(who: Who) = sequenceStudyWithChapter(studyId, position.chapterId) {
    case Study.WithChapter(study, chapter) => Contribute(who.u, study) {
      chapter.toggleGlyph(glyph, position.path) match {
        case Some(newChapter) =>
          studyRepo.updateNow(study)
          newChapter.root.nodeAt(position.path) ?? { node =>
            chapterRepo.setGlyphs(node.glyphs)(newChapter, position.path) >>-
              newChapter.root.nodeAt(position.path).foreach { node =>
                sendTo(study, StudySocket.SetGlyphs(position, node.glyphs, who.sri))
              }
          }
        case None =>
          fufail(s"Invalid toggleGlyph $studyId $position $glyph") >>-
            reloadSriBecauseOf(study, who.sri, chapter.id)
      }
    }
  }

  def setGamebook(studyId: Study.Id, position: Position.Ref, gamebook: Gamebook)(who: Who) = sequenceStudyWithChapter(studyId, position.chapterId) {
    case Study.WithChapter(study, chapter) => Contribute(who.u, study) {
      chapter.setGamebook(gamebook, position.path) match {
        case Some(newChapter) =>
          studyRepo.updateNow(study)
          chapterRepo.setGamebook(gamebook)(newChapter, position.path) >>- {
            indexStudy(study)
            sendStudyEnters(study, who.u)
          }
        case None =>
          fufail(s"Invalid setGamebook $studyId $position") >>-
            reloadSriBecauseOf(study, who.sri, chapter.id)
      }
    }
  }

  def explorerGame(studyId: Study.Id, data: actorApi.ExplorerGame)(who: Who) = sequenceStudyWithChapter(studyId, data.position.chapterId) {
    case Study.WithChapter(study, chapter) => Contribute(who.u, study) {
      if (data.insert) explorerGameHandler.insert(who.u, study, Position(chapter, data.position.path), data.gameId) flatMap {
        case None =>
          fufail(s"Invalid explorerGame insert $studyId $data") >>-
            reloadSriBecauseOf(study, who.sri, chapter.id)
        case Some((chapter, path)) =>
          studyRepo.updateNow(study)
          chapter.root.nodeAt(path) ?? { parent =>
            chapterRepo.setChildren(parent.children)(chapter, path) >>- {
              sendStudyEnters(study, who.u)
              sendTo(study, StudySocket.ReloadAll)
            }
          }
      }
      else explorerGameHandler.quote(data.gameId) flatMap {
        _ ?? {
          doSetComment(who.u, study, Position(chapter, data.position.path), _, who.sri)
        }
      }
    }
  }

  def addChapter(studyId: Study.Id, data: ChapterMaker.Data, sticky: Boolean)(who: Who) = sequenceStudy(studyId) { study =>
    Contribute(who.u, study) {
      chapterRepo.countByStudyId(study.id) flatMap { count =>
        if (count >= Study.maxChapters) funit
        else chapterRepo.nextOrderByStudy(study.id) flatMap { order =>
          chapterMaker(study, data, order, who.u) flatMap { chapter =>
            data.initial ?? {
              chapterRepo.firstByStudy(study.id) flatMap {
                _.filter(_.isEmptyInitial) ?? chapterRepo.delete
              }
            } >> doAddChapter(study, chapter, sticky, who.sri)
          } addFailureEffect {
            case ChapterMaker.ValidationException(error) =>
              sendTo(study, StudySocket.ValidationError(who.sri, error))
            case u => println(u)
          }
        }
      }
    }
  }

  def importPgns(studyId: Study.Id, datas: List[ChapterMaker.Data], sticky: Boolean)(who: Who) =
    lila.common.Future.applySequentially(datas) { data =>
      addChapter(studyId, data, sticky)(who)
    }

  def doAddChapter(study: Study, chapter: Chapter, sticky: Boolean, sri: Sri) =
    chapterRepo.insert(chapter) >> {
      val newStudy = study withChapter chapter
      (sticky ?? studyRepo.updateSomeFields(newStudy)) >>-
        sendTo(study, StudySocket.AddChapter(sri, newStudy.position, sticky))
    } >>-
      studyRepo.updateNow(study) >>-
      indexStudy(study)

  def setChapter(studyId: Study.Id, chapterId: Chapter.Id)(who: Who) = sequenceStudy(studyId) { study =>
    study.canContribute(who.u) ?? doSetChapter(study, chapterId, who.sri)
  }

  private def doSetChapter(study: Study, chapterId: Chapter.Id, sri: Sri) =
    (study.position.chapterId != chapterId) ?? {
      chapterRepo.byIdAndStudy(chapterId, study.id) flatMap {
        _ ?? { chapter =>
          val newStudy = study withChapter chapter
          studyRepo.updateSomeFields(newStudy) >>-
            sendTo(study, StudySocket.ChangeChapter(sri, newStudy.position))
        }
      }
    }

  def editChapter(studyId: Study.Id, data: ChapterMaker.EditData)(who: Who) = sequenceStudy(studyId) { study =>
    Contribute(who.u, study) {
      chapterRepo.byIdAndStudy(data.id, studyId) flatMap {
        _ ?? { chapter =>
          val name = Chapter fixName data.name
          val newChapter = chapter.copy(
            name = name,
            practice = data.isPractice option true,
            gamebook = data.isGamebook option true,
            conceal = (chapter.conceal, data.isConceal) match {
              case (None, true) => Chapter.Ply(chapter.root.ply).some
              case (Some(_), false) => None
              case _ => chapter.conceal
            },
            setup = chapter.setup.copy(orientation = data.realOrientation),
            description = data.hasDescription option {
              chapter.description | "-"
            }
          )
          if (chapter == newChapter) funit
          else chapterRepo.update(newChapter) >> {
            if (chapter.conceal != newChapter.conceal) {
              (newChapter.conceal.isDefined && study.position.chapterId == chapter.id).?? {
                val newPosition = study.position.withPath(Path.root)
                studyRepo.setPosition(study.id, newPosition)
              } >>-
                sendTo(study, StudySocket.ReloadAll)
            } else fuccess {
              val shouldReload =
                (newChapter.setup.orientation != chapter.setup.orientation) ||
                  (newChapter.practice != chapter.practice) ||
                  (newChapter.gamebook != chapter.gamebook) ||
                  (newChapter.description != chapter.description)
              if (shouldReload) sendTo(study, StudySocket.UpdateChapter(who.sri, chapter.id))
              else reloadChapters(study)
            }
          }
        } >>- indexStudy(study)
      }
    }
  }

  def descChapter(studyId: Study.Id, data: ChapterMaker.DescData)(who: Who) = sequenceStudy(studyId) { study =>
    Contribute(who.u, study) {
      chapterRepo.byIdAndStudy(data.id, studyId) flatMap {
        _ ?? { chapter =>
          val newChapter = chapter.copy(
            description = data.desc.nonEmpty option data.desc
          )
          (chapter != newChapter) ?? {
            chapterRepo.update(newChapter) >>- {
              sendTo(study, StudySocket.DescChapter(who.sri, newChapter.id, newChapter.description))
              indexStudy(study)
            }
          }
        }
      }
    }
  }

  def deleteChapter(studyId: Study.Id, chapterId: Chapter.Id)(who: Who) = sequenceStudy(studyId) { study =>
    Contribute(who.u, study) {
      chapterRepo.byIdAndStudy(chapterId, studyId) flatMap {
        _ ?? { chapter =>
          chapterRepo.orderedMetadataByStudy(studyId).flatMap { chaps =>
            // deleting the only chapter? Automatically create an empty one
            if (chaps.size < 2) {
              chapterMaker(study, ChapterMaker.Data(Chapter.Name("Chapter 1")), 1, who.u) flatMap { c =>
                doAddChapter(study, c, sticky = true, who.sri) >> doSetChapter(study, c.id, who.sri)
              }
            } // deleting the current chapter? Automatically move to another one
            else (study.position.chapterId == chapterId).?? {
              chaps.find(_.id != chapterId) ?? { newChap =>
                doSetChapter(study, newChap.id, who.sri)
              }
            }
          } >> chapterRepo.delete(chapter.id) >>- reloadChapters(study)
        } >>- indexStudy(study)
      }
    }
  }

  def sortChapters(studyId: Study.Id, chapterIds: List[Chapter.Id])(who: Who) = sequenceStudy(studyId) { study =>
    Contribute(who.u, study) {
      chapterRepo.sort(study, chapterIds) >>- reloadChapters(study)
    }
  }

  def descStudy(studyId: Study.Id, desc: String)(who: Who) = sequenceStudy(studyId) { study =>
    Contribute(who.u, study) {
      val newStudy = study.copy(description = desc.nonEmpty option desc)
      (study != newStudy) ?? {
        studyRepo.updateSomeFields(newStudy) >>-
          sendTo(study, StudySocket.DescStudy(who.sri, newStudy.description)) >>-
          indexStudy(study)
      }
    }
  }

  def editStudy(studyId: Study.Id, data: Study.Data)(who: Who) = sequenceStudy(studyId) { study =>
    data.settings.ifTrue(study isOwner who.u) ?? { settings =>
      val newStudy = study.copy(
        name = Study toName data.name,
        settings = settings,
        visibility = data.vis,
        description = settings.description option {
          study.description.filter(_.nonEmpty) | "-"
        }
      )
      if (!study.isPublic && newStudy.isPublic) {
        bus.publish(lila.hub.actorApi.study.StudyBecamePublic(studyId.value, study.members.contributorIds), 'study)
      } else if (study.isPublic && !newStudy.isPublic) {
        bus.publish(lila.hub.actorApi.study.StudyBecamePrivate(studyId.value, study.members.contributorIds), 'study)
      }
      (newStudy != study) ?? {
        studyRepo.updateSomeFields(newStudy) >>-
          sendTo(study, StudySocket.ReloadAll) >>-
          indexStudy(study) >>-
          lightStudyCache.put(studyId, newStudy.light.some)
      }
    }
  }

  def delete(study: Study) = sequenceStudy(study.id) { study =>
    studyRepo.delete(study) >>
      chapterRepo.deleteByStudy(study) >>-
      bus.publish(lila.hub.actorApi.study.RemoveStudy(study.id.value, study.members.contributorIds), 'study) >>-
      lightStudyCache.put(study.id, none)
  }

  def like(studyId: Study.Id, v: Boolean)(who: Who): Funit =
    studyRepo.like(studyId, who.u, v) map { likes =>
      sendToNew(studyId, _.SetLiking(Study.Liking(likes, v), who))
      bus.publish(actorApi.StudyLikes(studyId, likes), 'studyLikes)
      if (v) studyRepo byId studyId foreach {
        _ foreach { study =>
          if (who.u != study.ownerId && study.isPublic)
            timeline ! (Propagate(StudyLike(who.u, study.id.value, study.name.value)) toFollowersOf who.u)
        }
      }
    }

  def resetAllRanks = studyRepo.resetAllRanks

  def chapterIdNames(studyIds: List[Study.Id]): Fu[Map[Study.Id, Vector[Chapter.IdName]]] =
    chapterRepo.idNamesByStudyIds(studyIds, Study.maxChapters)

  def chapterMetadatas = chapterRepo.orderedMetadataByStudy _

  def withLiked(me: Option[User])(studies: Seq[Study]): Fu[Seq[Study.WithLiked]] =
    me.?? { u => studyRepo.filterLiked(u, studies.map(_.id)) } map { liked =>
      studies.map { study =>
        Study.WithLiked(study, liked(study.id))
      }
    }

  def analysisRequest(studyId: Study.Id, chapterId: Chapter.Id)(who: Who): Funit =
    sequenceStudyWithChapter(studyId, chapterId) {
      case Study.WithChapter(study, chapter) => Contribute(who.u, study) {
        serverEvalRequester(study, chapter, who.u)
      }
    }

  def erase(user: User) = studyRepo.allIdsByOwner(user.id) flatMap { ids =>
    chat ! lila.chat.actorApi.RemoveAll(ids.map(id => Chat.Id(id.value)))
    studyRepo.deleteByIds(ids) >>
      chapterRepo.deleteByStudyIds(ids)
  }

  private def sendStudyEnters(study: Study, userId: User.ID) = bus.publish(
    lila.hub.actorApi.study.StudyDoor(
      userId = userId,
      studyId = study.id.value,
      contributor = study canContribute userId,
      public = study.isPublic,
      enters = true
    ),
    'study
  )

  private def indexStudy(study: Study) =
    bus.publish(actorApi.SaveStudy(study), 'study)

  private def reloadSri(study: Study, sri: Sri, becauseOf: Option[Chapter.Id] = None) =
    sendTo(study, StudySocket.ReloadSri(sri))

  private def reloadSriBecauseOf(study: Study, sri: Sri, chapterId: Chapter.Id) =
    sendTo(study, StudySocket.ReloadSriBecauseOf(sri, chapterId))

  private def reloadChapters(study: Study) =
    chapterRepo.orderedMetadataByStudy(study.id).foreach { chapters =>
      sendTo(study, StudySocket.ReloadChapters(chapters))
    }

  import ornicar.scalalib.Zero
  private def Contribute[A](userId: User.ID, study: Study)(f: => A)(implicit default: Zero[A]): A =
    if (study canContribute userId) f else default.zero

  private def sendTo(study: Study, msg: Any): Unit = sendTo(study.id, msg)

  private def sendTo(studyId: Study.Id, msg: Any): Unit =
    socketMap.tell(studyId.value, msg)

  private def sendToNew(studyId: Study.Id, msg: StudyRemoteSocket.Out.type => StudyRemoteSocket.Out): Unit =
    bus.publish(StudyRemoteSocket.Send(studyId, msg(StudyRemoteSocket.Out)), 'studySocket)
}
