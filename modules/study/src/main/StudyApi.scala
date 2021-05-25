package lila.study

import actorApi.Who
import akka.stream.scaladsl._
import chess.Centis
import chess.format.pgn.{ Glyph, Tags }
import scala.concurrent.duration._

import lila.chat.{ Chat, ChatApi }
import lila.common.Bus
import lila.hub.actorApi.timeline.{ Propagate, StudyCreate, StudyLike }
import lila.socket.Socket.Sri
import lila.tree.Node.{ Comment, Gamebook, Shapes }
import lila.user.{ Holder, User }

final class StudyApi(
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo,
    sequencer: StudySequencer,
    studyMaker: StudyMaker,
    chapterMaker: ChapterMaker,
    inviter: StudyInvite,
    explorerGameHandler: ExplorerGame,
    topicApi: StudyTopicApi,
    lightUserApi: lila.user.LightUserApi,
    scheduler: akka.actor.Scheduler,
    chatApi: ChatApi,
    timeline: lila.hub.actors.Timeline,
    serverEvalRequester: ServerEval.Requester
)(implicit
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer
) {

  import sequencer._

  def byId = studyRepo byId _

  def byIds = studyRepo byOrderedIds _

  def publicIdNames = studyRepo publicIdNames _

  def publicByIds(ids: Seq[Study.Id]) = byIds(ids) map { _.filter(_.isPublic) }

  def byIdAndOwner(id: Study.Id, owner: User) =
    byId(id) map {
      _.filter(_ isOwner owner.id)
    }

  def isOwner(id: Study.Id, owner: User) = byIdAndOwner(id, owner).map(_.isDefined)

  def byIdWithChapter(id: Study.Id): Fu[Option[Study.WithChapter]] =
    byId(id) flatMap {
      _ ?? { study =>
        chapterRepo byId study.position.chapterId flatMap {
          case None =>
            chapterRepo firstByStudy study.id flatMap {
              case None => fixNoChapter(study)
              case Some(chapter) =>
                val fixed = study withChapter chapter
                studyRepo updateSomeFields fixed inject
                  Study.WithChapter(fixed, chapter).some
            }
          case Some(chapter) => fuccess(Study.WithChapter(study, chapter).some)
        }
      }
    }

  def byIdWithChapter(id: Study.Id, chapterId: Chapter.Id): Fu[Option[Study.WithChapter]] =
    byId(id) flatMap {
      _ ?? { study =>
        chapterRepo byId chapterId map {
          _.filter(_.studyId == study.id) map { Study.WithChapter(study, _) }
        } orElse byIdWithChapter(id)
      }
    }

  def byIdWithFirstChapter(id: Study.Id): Fu[Option[Study.WithChapter]] =
    byIdWithChapterFinder(id, chapterRepo firstByStudy id)

  private[study] def byIdWithLastChapter(id: Study.Id): Fu[Option[Study.WithChapter]] =
    byIdWithChapterFinder(id, chapterRepo lastByStudy id)

  private def byIdWithChapterFinder(
      id: Study.Id,
      chapterFinder: => Fu[Option[Chapter]]
  ): Fu[Option[Study.WithChapter]] =
    byId(id) flatMap {
      _ ?? { study =>
        chapterFinder map {
          _ ?? { Study.WithChapter(study, _).some }
        } orElse byIdWithChapter(id)
      }
    }

  private def fixNoChapter(study: Study): Fu[Option[Study.WithChapter]] =
    sequenceStudy(study.id) { study =>
      chapterRepo existsByStudy study.id flatMap {
        case true => funit
        case _ =>
          chapterMaker.fromFenOrPgnOrBlank(
            study,
            ChapterMaker.Data(Chapter.Name("Chapter 1")),
            order = 1,
            userId = study.ownerId
          ) flatMap chapterRepo.insert
      }
    } >> byIdWithFirstChapter(study.id)

  def studyIdOf = chapterRepo.studyIdOf _

  def members(id: Study.Id): Fu[Option[StudyMembers]] = studyRepo membersById id

  def importGame(data: StudyMaker.ImportGame, user: User): Fu[Option[Study.WithChapter]] =
    (data.form.as match {
      case StudyForm.importGame.AsNewStudy => create(data, user)
      case StudyForm.importGame.AsChapterOf(studyId) =>
        byId(studyId) flatMap {
          case Some(study) if study.canContribute(user.id) =>
            addChapter(
              studyId = study.id,
              data = data.form.toChapterData,
              sticky = study.settings.sticky
            )(Who(user.id, Sri(""))) >> byIdWithLastChapter(studyId)
          case _ => fuccess(none)
        } orElse importGame(data.copy(form = data.form.copy(asStr = none)), user)
    }) addEffect {
      _ ?? { sc =>
        Bus.publish(actorApi.StartStudy(sc.study.id), "startStudy")
      }
    }

  def create(
      data: StudyMaker.ImportGame,
      user: User,
      transform: Study => Study = identity
  ): Fu[Option[Study.WithChapter]] =
    studyMaker(data, user) map { sc =>
      sc.copy(study = transform(sc.study))
    } flatMap { sc =>
      studyRepo.insert(sc.study) >>
        chapterRepo.insert(sc.chapter) >>-
        indexStudy(sc.study) >>-
        scheduleTimeline(sc.study.id) inject sc.some
    }

  def clone(me: User, prev: Study): Fu[Option[Study]] =
    Settings.UserSelection.allows(prev.settings.cloneable, prev, me.id.some) ?? {
      val study1 = prev.cloneFor(me)
      chapterRepo
        .orderedByStudySource(prev.id)
        .map(_ cloneFor study1)
        .mapAsync(4) { c =>
          chapterRepo.insert(c) inject c
        }
        .toMat(Sink.reduce[Chapter] { case (prev, _) => prev })(Keep.right)
        .run()
        .flatMap { (first: Chapter) =>
          val study = study1 rewindTo first
          studyRepo.insert(study) >>
            chatApi.userChat.system(
              Chat.Id(study.id.value),
              s"Cloned from lichess.org/study/${prev.id}",
              _.Study
            ) inject study.some
        }
    }

  def resetIfOld(study: Study, chapters: List[Chapter.Metadata]): Fu[(Study, Option[Chapter])] =
    chapters.headOption match {
      case Some(c) if study.isOld && study.position != c.initialPosition =>
        val newStudy = study rewindTo c
        studyRepo.updateSomeFields(newStudy) zip chapterRepo.byId(c.id) map { case (_, chapter) =>
          newStudy -> chapter
        }
      case _ => fuccess(study -> none)
    }

  private def scheduleTimeline(studyId: Study.Id): Unit =
    scheduler
      .scheduleOnce(1 minute) {
        byId(studyId) foreach {
          _.withFilter(_.isPublic) foreach { study =>
            timeline ! (Propagate(
              StudyCreate(study.ownerId, study.id.value, study.name.value)
            ) toFollowersOf study.ownerId)
          }
        }
      }
      .unit

  def talk(userId: User.ID, studyId: Study.Id, text: String) =
    byId(studyId) foreach {
      _ foreach { study =>
        (study canChat userId) ?? {
          chatApi.userChat.write(
            Chat.Id(studyId.value),
            userId = userId,
            text = text,
            publicSource = lila.hub.actorApi.shutup.PublicSource.Study(studyId.value).some,
            busChan = _.Study
          )
        }
      }
    }

  def setPath(studyId: Study.Id, position: Position.Ref)(who: Who): Funit =
    sequenceStudy(studyId) { study =>
      Contribute(who.u, study) {
        chapterRepo.byId(position.chapterId).map {
          _ filter { c =>
            c.root.pathExists(position.path) && study.position.chapterId == c.id
          }
        } flatMap {
          case None => funit >>- sendTo(study.id)(_.reloadSri(who.sri))
          case Some(chapter) if study.position.path != position.path =>
            studyRepo.setPosition(study.id, position) >>
              updateConceal(study, chapter, position) >>-
              sendTo(study.id)(_.setPath(position, who))
          case _ => funit
        }
      }
    }

  def addNode(
      studyId: Study.Id,
      position: Position.Ref,
      node: Node,
      opts: MoveOpts,
      relay: Option[Chapter.Relay] = None
  )(who: Who): Funit =
    sequenceStudyWithChapter(studyId, position.chapterId) { case Study.WithChapter(study, chapter) =>
      Contribute(who.u, study) {
        doAddNode(study, Position(chapter, position.path), node, opts, relay)(who)
      }
    } flatMap { _ ?? { _() } } // this one is for you, Lakin <3

  private def doAddNode(
      study: Study,
      position: Position,
      rawNode: Node,
      opts: MoveOpts,
      relay: Option[Chapter.Relay]
  )(who: Who): Fu[Option[() => Funit]] = {
    val singleNode   = rawNode.withoutChildren
    def failReload() = reloadSriBecauseOf(study, who.sri, position.chapter.id)
    if (position.chapter.isOverweight) {
      logger.info(s"Overweight chapter ${study.id}/${position.chapter.id}")
      failReload()
      fuccess(none)
    } else
      position.chapter.addNode(singleNode, position.path, relay) match {
        case None =>
          failReload()
          fufail(s"Invalid addNode ${study.id} ${position.ref} $singleNode")
        case Some(chapter) =>
          chapter.root.nodeAt(position.path) ?? { parent =>
            parent.children.get(singleNode.id) ?? { node =>
              val newPosition = position.ref + node
              chapterRepo.addSubTree(node, parent addChild node, position.path)(chapter) >>
                (relay ?? { chapterRepo.setRelay(chapter.id, _) }) >>
                (opts.sticky ?? studyRepo.setPosition(study.id, newPosition)) >>
                updateConceal(study, chapter, newPosition) >>-
                sendTo(study.id)(
                  _.addNode(
                    position.ref,
                    node,
                    chapter.setup.variant,
                    sticky = opts.sticky,
                    relay = relay,
                    who
                  )
                ) inject {
                  (opts.promoteToMainline && !Path.isMainline(chapter.root, newPosition.path)) option { () =>
                    promote(study.id, position.ref + node, toMainline = true)(who)
                  }
                }
            }
          }
      }
  }

  private def updateConceal(study: Study, chapter: Chapter, position: Position.Ref) =
    chapter.conceal ?? { conceal =>
      chapter.root.lastMainlinePlyOf(position.path).some.filter(_ > conceal) ?? { newConceal =>
        if (newConceal >= chapter.root.lastMainlinePly)
          chapterRepo.removeConceal(chapter.id) >>-
            sendTo(study.id)(_.setConceal(position, none))
        else
          chapterRepo.setConceal(chapter.id, newConceal) >>-
            sendTo(study.id)(_.setConceal(position, newConceal.some))
      }
    }

  def deleteNodeAt(studyId: Study.Id, position: Position.Ref)(who: Who) =
    sequenceStudyWithChapter(studyId, position.chapterId) { case Study.WithChapter(study, chapter) =>
      Contribute(who.u, study) {
        chapter.updateRoot { root =>
          root.withChildren(_.deleteNodeAt(position.path))
        } match {
          case Some(newChapter) =>
            chapterRepo.update(newChapter) >>-
              sendTo(study.id)(_.deleteNode(position, who))
          case None =>
            fufail(s"Invalid delNode $studyId $position") >>-
              reloadSriBecauseOf(study, who.sri, chapter.id)
        }
      }
    }

  def clearAnnotations(studyId: Study.Id, chapterId: Chapter.Id)(who: Who) =
    sequenceStudyWithChapter(studyId, chapterId) { case Study.WithChapter(study, chapter) =>
      Contribute(who.u, study) {
        chapterRepo.update(chapter.updateRoot { root =>
          root.withChildren(_.updateAllWith(_.clearAnnotations).some)
        } | chapter) >>- sendTo(study.id)(_.updateChapter(chapter.id, who))
      }
    }

  // rewrites the whole chapter because of `forceVariation`. Very inefficient.
  def promote(studyId: Study.Id, position: Position.Ref, toMainline: Boolean)(who: Who): Funit =
    sequenceStudyWithChapter(studyId, position.chapterId) { case Study.WithChapter(study, chapter) =>
      Contribute(who.u, study) {
        chapter.updateRoot { root =>
          root.withChildren { children =>
            if (toMainline) children.promoteToMainlineAt(position.path)
            else children.promoteUpAt(position.path).map(_._1)
          }
        } match {
          case Some(newChapter) =>
            chapterRepo.update(newChapter) >>-
              sendTo(study.id)(_.promote(position, toMainline, who)) >>
              newChapter.root.children
                .nodesOn {
                  newChapter.root.mainlinePath.intersect(position.path)
                }
                .collect {
                  case (node, path) if node.forceVariation =>
                    doForceVariation(Study.WithChapter(study, newChapter), path, force = false, who)
                }
                .sequenceFu
                .void
          case None =>
            fufail(s"Invalid promoteToMainline $studyId $position") >>-
              reloadSriBecauseOf(study, who.sri, chapter.id)
        }
      }
    }

  def forceVariation(studyId: Study.Id, position: Position.Ref, force: Boolean)(who: Who): Funit =
    sequenceStudyWithChapter(studyId, position.chapterId) { sc =>
      Contribute(who.u, sc.study) {
        doForceVariation(sc, position.path, force, who)
      }
    }

  private def doForceVariation(sc: Study.WithChapter, path: Path, force: Boolean, who: Who): Funit =
    sc.chapter.forceVariation(force, path) match {
      case Some(newChapter) =>
        chapterRepo.forceVariation(force)(newChapter, path) >>-
          sendTo(sc.study.id)(_.forceVariation(Position(newChapter, path).ref, force, who))
      case None =>
        fufail(s"Invalid forceVariation ${Position(sc.chapter, path)} $force") >>-
          reloadSriBecauseOf(sc.study, who.sri, sc.chapter.id)
    }

  def setRole(studyId: Study.Id, userId: User.ID, roleStr: String)(who: Who) =
    sequenceStudy(studyId) { study =>
      canActAsOwner(study, who.u) flatMap {
        _ ?? {
          val role    = StudyMember.Role.byId.getOrElse(roleStr, StudyMember.Role.Read)
          val members = study.members.update(userId, _.copy(role = role))
          studyRepo.setRole(study, userId, role) >>- onMembersChange(study, members, members.ids)
        }
      }
    }

  def invite(
      byUserId: User.ID,
      studyId: Study.Id,
      username: String,
      isPresent: User.ID => Fu[Boolean],
      onError: String => Unit
  ) =
    sequenceStudy(studyId) { study =>
      inviter(byUserId, study, username, isPresent)
        .addEffects(
          err => onError(err.getMessage),
          user => {
            val members = study.members + StudyMember.make(user)
            onMembersChange(study, members, members.ids)
          }
        )
        .void
    }

  def kick(studyId: Study.Id, userId: User.ID)(who: Who) =
    sequenceStudy(studyId) { study =>
      studyRepo.isAdminMember(study, who.u) flatMap { isAdmin =>
        val allowed = study.isMember(userId) && {
          (isAdmin && !study.isOwner(userId)) || (study.isOwner(who.u) ^ (who.u == userId))
        }
        allowed ?? {
          studyRepo.removeMember(study, userId) >>-
            onMembersChange(study, (study.members - userId), study.members.ids)
        }
      }
    }

  def isContributor = studyRepo.isContributor _
  def isMember      = studyRepo.isMember _

  private def onMembersChange(
      study: Study,
      members: StudyMembers,
      sendToUserIds: Iterable[User.ID]
  ): Unit = {
    sendTo(study.id)(_.reloadMembers(members, sendToUserIds))
    indexStudy(study)
  }

  def setShapes(studyId: Study.Id, position: Position.Ref, shapes: Shapes)(who: Who) =
    sequenceStudy(studyId) { study =>
      Contribute(who.u, study) {
        chapterRepo.byIdAndStudy(position.chapterId, study.id) flatMap {
          _ ?? { chapter =>
            chapter.setShapes(shapes, position.path) match {
              case Some(newChapter) =>
                studyRepo.updateNow(study)
                chapterRepo.setShapes(shapes)(newChapter, position.path) >>-
                  sendTo(study.id)(_.setShapes(position, shapes, who))
              case None =>
                fufail(s"Invalid setShapes $position $shapes") >>-
                  reloadSriBecauseOf(study, who.sri, chapter.id)
            }
          }
        }
      }
    }

  def setClock(studyId: Study.Id, position: Position.Ref, clock: Option[Centis])(who: Who): Funit =
    sequenceStudyWithChapter(studyId, position.chapterId) {
      doSetClock(_, position, clock)(who)
    }

  private def doSetClock(sc: Study.WithChapter, position: Position.Ref, clock: Option[Centis])(
      who: Who
  ): Funit =
    sc.chapter.setClock(clock, position.path) match {
      case Some(newChapter) =>
        studyRepo.updateNow(sc.study)
        chapterRepo.setClock(clock)(newChapter, position.path) >>-
          sendTo(sc.study.id)(_.setClock(position, clock, who))
      case None =>
        fufail(s"Invalid setClock $position $clock") >>-
          reloadSriBecauseOf(sc.study, who.sri, position.chapterId)
    }

  def setTag(studyId: Study.Id, setTag: actorApi.SetTag)(who: Who) =
    sequenceStudyWithChapter(studyId, setTag.chapterId) { case Study.WithChapter(study, chapter) =>
      Contribute(who.u, study) {
        doSetTags(study, chapter, PgnTags(chapter.tags + setTag.tag), who)
      }
    }

  def setTags(studyId: Study.Id, chapterId: Chapter.Id, tags: Tags)(who: Who) =
    sequenceStudyWithChapter(studyId, chapterId) { case Study.WithChapter(study, chapter) =>
      Contribute(who.u, study) {
        doSetTags(study, chapter, tags, who)
      }
    }

  private def doSetTags(study: Study, oldChapter: Chapter, tags: Tags, who: Who): Funit = {
    val chapter = oldChapter.copy(tags = tags)
    (chapter.tags != oldChapter.tags) ?? {
      chapterRepo.setTagsFor(chapter) >> {
        PgnTags.setRootClockFromTags(chapter) ?? { c =>
          doSetClock(Study.WithChapter(study, c), Position(c, Path.root).ref, c.root.clock)(who)
        }
      } >>-
        sendTo(study.id)(_.setTags(chapter.id, chapter.tags, who))
    } >>- indexStudy(study)
  }

  def setComment(studyId: Study.Id, position: Position.Ref, text: Comment.Text)(who: Who) =
    sequenceStudyWithChapter(studyId, position.chapterId) { case Study.WithChapter(study, chapter) =>
      Contribute(who.u, study) {
        lightUserApi.async(who.u) flatMap {
          _ ?? { author =>
            val comment = Comment(
              id = Comment.Id.make,
              text = text,
              by = Comment.Author.User(author.id, author.titleName)
            )
            doSetComment(study, Position(chapter, position.path), comment, who)
          }
        }
      }
    }

  private def doSetComment(study: Study, position: Position, comment: Comment, who: Who): Funit =
    position.chapter.setComment(comment, position.path) match {
      case Some(newChapter) =>
        studyRepo.updateNow(study)
        newChapter.root.nodeAt(position.path) ?? { node =>
          node.comments.findBy(comment.by) ?? { c =>
            chapterRepo.setComments(node.comments.filterEmpty)(newChapter, position.path) >>- {
              sendTo(study.id)(_.setComment(position.ref, c, who))
              indexStudy(study)
            }
          }
        }
      case None =>
        fufail(s"Invalid setComment ${study.id} $position") >>-
          reloadSriBecauseOf(study, who.sri, position.chapter.id)
    }

  def deleteComment(studyId: Study.Id, position: Position.Ref, id: Comment.Id)(who: Who) =
    sequenceStudyWithChapter(studyId, position.chapterId) { case Study.WithChapter(study, chapter) =>
      Contribute(who.u, study) {
        chapter.deleteComment(id, position.path) match {
          case Some(newChapter) =>
            chapterRepo.update(newChapter) >>-
              sendTo(study.id)(_.deleteComment(position, id, who)) >>-
              indexStudy(study)
          case None =>
            fufail(s"Invalid deleteComment $studyId $position $id") >>-
              reloadSriBecauseOf(study, who.sri, chapter.id)
        }
      }
    }

  def toggleGlyph(studyId: Study.Id, position: Position.Ref, glyph: Glyph)(who: Who) =
    sequenceStudyWithChapter(studyId, position.chapterId) { case Study.WithChapter(study, chapter) =>
      Contribute(who.u, study) {
        chapter.toggleGlyph(glyph, position.path) match {
          case Some(newChapter) =>
            studyRepo.updateNow(study)
            newChapter.root.nodeAt(position.path) ?? { node =>
              chapterRepo.setGlyphs(node.glyphs)(newChapter, position.path) >>-
                newChapter.root.nodeAt(position.path).foreach { node =>
                  sendTo(study.id)(_.setGlyphs(position, node.glyphs, who))
                }
            }
          case None =>
            fufail(s"Invalid toggleGlyph $studyId $position $glyph") >>-
              reloadSriBecauseOf(study, who.sri, chapter.id)
        }
      }
    }

  def setGamebook(studyId: Study.Id, position: Position.Ref, gamebook: Gamebook)(who: Who) =
    sequenceStudyWithChapter(studyId, position.chapterId) { case Study.WithChapter(study, chapter) =>
      Contribute(who.u, study) {
        chapter.setGamebook(gamebook, position.path) match {
          case Some(newChapter) =>
            studyRepo.updateNow(study)
            chapterRepo.setGamebook(gamebook)(newChapter, position.path) >>-
              indexStudy(study)
          case None =>
            fufail(s"Invalid setGamebook $studyId $position") >>-
              reloadSriBecauseOf(study, who.sri, chapter.id)
        }
      }
    }

  def explorerGame(studyId: Study.Id, data: actorApi.ExplorerGame)(who: Who) =
    sequenceStudyWithChapter(studyId, data.position.chapterId) { case Study.WithChapter(study, chapter) =>
      Contribute(who.u, study) {
        if (data.insert)
          explorerGameHandler.insert(study, Position(chapter, data.position.path), data.gameId) flatMap {
            case None =>
              fufail(s"Invalid explorerGame insert $studyId $data") >>-
                reloadSriBecauseOf(study, who.sri, chapter.id)
            case Some((chapter, path)) =>
              studyRepo.updateNow(study)
              chapter.root.nodeAt(path) ?? { parent =>
                chapterRepo.setChildren(parent.children)(chapter, path) >>-
                  sendTo(study.id)(_.reloadAll)
              }
          }
        else
          explorerGameHandler.quote(data.gameId) flatMap {
            _ ?? {
              doSetComment(study, Position(chapter, data.position.path), _, who)
            }
          }
      }
    }

  def addChapter(studyId: Study.Id, data: ChapterMaker.Data, sticky: Boolean)(who: Who): Funit =
    data.manyGames match {
      case Some(datas) =>
        lila.common.Future.applySequentially(datas) { data =>
          addChapter(studyId, data, sticky)(who)
        }
      case _ =>
        sequenceStudy(studyId) { study =>
          Contribute(who.u, study) {
            chapterRepo.countByStudyId(study.id) flatMap { count =>
              if (count >= Study.maxChapters) funit
              else
                chapterRepo.nextOrderByStudy(study.id) flatMap { order =>
                  chapterMaker(study, data, order, who.u) flatMap { chapter =>
                    data.initial ?? {
                      chapterRepo.firstByStudy(study.id) flatMap {
                        _.filter(_.isEmptyInitial) ?? chapterRepo.delete
                      }
                    } >> doAddChapter(study, chapter, sticky, who)
                  } addFailureEffect {
                    case ChapterMaker.ValidationException(error) =>
                      sendTo(study.id)(_.validationError(error, who.sri))
                    case u => logger.error(s"StudyApi.addChapter to $studyId", u)
                  }
                }
            }
          }
        }
    }

  def rename(studyId: Study.Id, name: Study.Name): Funit =
    sequenceStudy(studyId) { old =>
      val study = old.copy(name = name)
      studyRepo.updateSomeFields(study) >>- indexStudy(study)
    }

  def importPgns(studyId: Study.Id, datas: List[ChapterMaker.Data], sticky: Boolean)(who: Who) =
    lila.common.Future.applySequentially(datas) { data =>
      addChapter(studyId, data, sticky)(who)
    }

  def doAddChapter(study: Study, chapter: Chapter, sticky: Boolean, who: Who) =
    chapterRepo.insert(chapter) >> {
      val newStudy = study withChapter chapter
      (sticky ?? studyRepo.updateSomeFields(newStudy)) >>-
        sendTo(study.id)(_.addChapter(newStudy.position, sticky, who))
    } >>
      studyRepo.updateNow(study) >>-
      indexStudy(study)

  def setChapter(studyId: Study.Id, chapterId: Chapter.Id)(who: Who) =
    sequenceStudy(studyId) { study =>
      study.canContribute(who.u) ?? doSetChapter(study, chapterId, who)
    }

  private def doSetChapter(study: Study, chapterId: Chapter.Id, who: Who) =
    (study.position.chapterId != chapterId) ?? {
      chapterRepo.byIdAndStudy(chapterId, study.id) flatMap {
        _ ?? { chapter =>
          val newStudy = study withChapter chapter
          studyRepo.updateSomeFields(newStudy) >>-
            sendTo(study.id)(_.changeChapter(newStudy.position, who))
        }
      }
    }

  def editChapter(studyId: Study.Id, data: ChapterMaker.EditData)(who: Who) =
    sequenceStudy(studyId) { study =>
      Contribute(who.u, study) {
        chapterRepo.byIdAndStudy(data.id, studyId) flatMap {
          _ ?? { chapter =>
            val name = Chapter fixName data.name
            val newChapter = chapter.copy(
              name = name,
              practice = data.isPractice option true,
              gamebook = data.isGamebook option true,
              conceal = (chapter.conceal, data.isConceal) match {
                case (None, true)     => Chapter.Ply(chapter.root.ply).some
                case (Some(_), false) => None
                case _                => chapter.conceal
              },
              setup = chapter.setup.copy(
                orientation = data.realOrientation match {
                  case ChapterMaker.Orientation.Fixed(color) => color
                  case _                                     => chapter.setup.orientation
                }
              ),
              description = data.hasDescription option {
                chapter.description | "-"
              }
            )
            if (chapter == newChapter) funit
            else
              chapterRepo.update(newChapter) >> {
                if (chapter.conceal != newChapter.conceal) {
                  (newChapter.conceal.isDefined && study.position.chapterId == chapter.id).?? {
                    val newPosition = study.position.withPath(Path.root)
                    studyRepo.setPosition(study.id, newPosition)
                  } >>-
                    sendTo(study.id)(_.reloadAll)
                } else
                  fuccess {
                    val shouldReload =
                      (newChapter.setup.orientation != chapter.setup.orientation) ||
                        (newChapter.practice != chapter.practice) ||
                        (newChapter.gamebook != chapter.gamebook) ||
                        (newChapter.description != chapter.description)
                    if (shouldReload) sendTo(study.id)(_.updateChapter(chapter.id, who))
                    else reloadChapters(study)
                  }
              }
          } >>- indexStudy(study)
        }
      }
    }

  def descChapter(studyId: Study.Id, data: ChapterMaker.DescData)(who: Who) =
    sequenceStudy(studyId) { study =>
      Contribute(who.u, study) {
        chapterRepo.byIdAndStudy(data.id, studyId) flatMap {
          _ ?? { chapter =>
            val newChapter = chapter.copy(
              description = data.desc.nonEmpty option data.desc
            )
            (chapter != newChapter) ?? {
              chapterRepo.update(newChapter) >>- {
                sendTo(study.id)(_.descChapter(newChapter.id, newChapter.description, who))
                indexStudy(study)
              }
            }
          }
        }
      }
    }

  def deleteChapter(studyId: Study.Id, chapterId: Chapter.Id)(who: Who) =
    sequenceStudy(studyId) { study =>
      Contribute(who.u, study) {
        chapterRepo.byIdAndStudy(chapterId, studyId) flatMap {
          _ ?? { chapter =>
            chapterRepo.orderedMetadataByStudy(studyId).flatMap { chaps =>
              // deleting the only chapter? Automatically create an empty one
              if (chaps.sizeIs < 2) {
                chapterMaker(study, ChapterMaker.Data(Chapter.Name("Chapter 1")), 1, who.u) flatMap { c =>
                  doAddChapter(study, c, sticky = true, who) >> doSetChapter(study, c.id, who)
                }
              } // deleting the current chapter? Automatically move to another one
              else
                (study.position.chapterId == chapterId).?? {
                  chaps.find(_.id != chapterId) ?? { newChap =>
                    doSetChapter(study, newChap.id, who)
                  }
                }
            } >> chapterRepo.delete(chapter.id) >>- reloadChapters(study)
          } >>- indexStudy(study)
        }
      }
    }

  def sortChapters(studyId: Study.Id, chapterIds: List[Chapter.Id])(who: Who) =
    sequenceStudy(studyId) { study =>
      Contribute(who.u, study) {
        chapterRepo.sort(study, chapterIds) >>- reloadChapters(study)
      }
    }

  def descStudy(studyId: Study.Id, desc: String)(who: Who) =
    sequenceStudy(studyId) { study =>
      Contribute(who.u, study) {
        val newStudy = study.copy(description = desc.nonEmpty option desc)
        (study != newStudy) ?? {
          studyRepo.updateSomeFields(newStudy) >>-
            sendTo(study.id)(_.descStudy(newStudy.description, who)) >>-
            indexStudy(study)
        }
      }
    }

  def setTopics(studyId: Study.Id, topicStrs: List[String])(who: Who) =
    sequenceStudy(studyId) { study =>
      Contribute(who.u, study) {
        val topics    = StudyTopics.fromStrs(topicStrs)
        val newStudy  = study.copy(topics = topics.some)
        val newTopics = study.topics.fold(topics)(topics.diff)
        (study != newStudy) ?? {
          studyRepo.updateTopics(newStudy) >>
            topicApi.userTopicsAdd(who.u, newTopics) >>- {
              sendTo(study.id)(_.setTopics(topics, who))
              indexStudy(study)
              topicApi.recompute()
            }
        }
      }
    }

  def addTopics(studyId: Study.Id, topics: List[String]) =
    sequenceStudy(studyId) { study =>
      studyRepo.updateTopics(study addTopics StudyTopics.fromStrs(topics))
    }

  def editStudy(studyId: Study.Id, data: Study.Data)(who: Who) =
    sequenceStudy(studyId) { study =>
      canActAsOwner(study, who.u) flatMap { asOwner =>
        data.settings.ifTrue(asOwner) ?? { settings =>
          val newStudy = study.copy(
            name = Study toName data.name,
            settings = settings,
            visibility = data.vis,
            description = settings.description option {
              study.description.filter(_.nonEmpty) | "-"
            }
          )
          (newStudy != study) ?? {
            studyRepo.updateSomeFields(newStudy) >>-
              sendTo(study.id)(_.reloadAll) >>-
              indexStudy(study)
          }
        }
      }
    }

  def delete(study: Study) =
    sequenceStudy(study.id) { study =>
      studyRepo.delete(study) >>
        chapterRepo.deleteByStudy(study)
    }

  def like(studyId: Study.Id, v: Boolean)(who: Who): Funit =
    studyRepo.like(studyId, who.u, v) map { likes =>
      sendTo(studyId)(_.setLiking(Study.Liking(likes, v), who))
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
    me.?? { u =>
      studyRepo.filterLiked(u, studies.map(_.id))
    } map { liked =>
      studies.map { study =>
        Study.WithLiked(study, liked(study.id))
      }
    }

  def analysisRequest(studyId: Study.Id, chapterId: Chapter.Id, userId: User.ID): Funit =
    sequenceStudyWithChapter(studyId, chapterId) { case Study.WithChapter(study, chapter) =>
      Contribute(userId, study) {
        serverEvalRequester(study, chapter, userId)
      }
    }

  def deleteAllChapters(studyId: Study.Id, by: User) =
    sequenceStudy(studyId) { study =>
      Contribute(by.id, study) {
        chapterRepo deleteByStudy study
      }
    }

  def adminInvite(studyId: Study.Id, me: Holder): Funit =
    sequenceStudy(studyId) { inviter.admin(_, me) }

  private def indexStudy(study: Study) =
    Bus.publish(actorApi.SaveStudy(study), "study")

  private def reloadSriBecauseOf(study: Study, sri: Sri, chapterId: Chapter.Id) =
    sendTo(study.id)(_.reloadSriBecauseOf(sri, chapterId))

  def reloadChapters(study: Study) =
    chapterRepo.orderedMetadataByStudy(study.id).foreach { chapters =>
      sendTo(study.id)(_ reloadChapters chapters)
    }

  private def canActAsOwner(study: Study, userId: User.ID): Fu[Boolean] =
    fuccess(study isOwner userId) >>| studyRepo.isAdminMember(study, userId)

  import ornicar.scalalib.Zero
  private def Contribute[A](userId: User.ID, study: Study)(f: => A)(implicit default: Zero[A]): A =
    if (study canContribute userId) f else default.zero

  // work around circular dependency
  private var socket: Option[StudySocket] = None
  private[study] def registerSocket(s: StudySocket) = { socket = s.some }
  private def sendTo(studyId: Study.Id)(f: StudySocket => Study.Id => Unit): Unit =
    socket foreach { s =>
      f(s)(studyId)
    }
}
