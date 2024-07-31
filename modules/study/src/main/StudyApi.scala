package lila.study

import akka.stream.scaladsl.*
import chess.Centis
import chess.format.UciPath
import chess.format.pgn.{ Glyph, Tags }

import lila.common.Bus
import lila.core.perm.Granter
import lila.core.socket.Sri
import lila.core.study as hub
import lila.core.timeline.{ Propagate, StudyLike }
import lila.tree.Branch
import lila.tree.Node.{ Comment, Gamebook, Shapes }

import actorApi.Who

final class StudyApi(
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo,
    sequencer: StudySequencer,
    studyMaker: StudyMaker,
    chapterMaker: ChapterMaker,
    inviter: StudyInvite,
    explorerGameHandler: ExplorerGame,
    topicApi: StudyTopicApi,
    lightUserApi: lila.core.user.LightUserApi,
    chatApi: lila.core.chat.ChatApi,
    serverEvalRequester: ServerEval.Requester,
    preview: ChapterPreviewApi,
    flairApi: lila.core.user.FlairApi
)(using Executor, akka.stream.Materializer)
    extends lila.core.study.StudyApi:

  import sequencer.*

  export studyRepo.{ byId, byOrderedIds as byIds, publicIdNames }

  def publicByIds(ids: Seq[StudyId]) = byIds(ids).map { _.filter(_.isPublic) }

  def byIdAndOwner(id: StudyId, owner: User) =
    byId(id).map:
      _.filter(_.isOwner(owner.id))

  def isOwner(id: StudyId, owner: User) = byIdAndOwner(id, owner).map(_.isDefined)

  def byIdAndOwnerOrAdmin(id: StudyId, owner: User) =
    byId(id).map:
      _.filter(_.isOwner(owner.id) || Granter.ofUser(_.StudyAdmin)(owner))

  def isOwnerOrAdmin(id: StudyId, owner: User) = byIdAndOwnerOrAdmin(id, owner).map(_.isDefined)

  def byIdWithChapter(id: StudyId): Fu[Option[Study.WithChapter]] =
    byId(id).flatMapz: study =>
      chapterRepo.byId(study.position.chapterId).flatMap {
        case None =>
          chapterRepo.firstByStudy(study.id).flatMap {
            case None => fixNoChapter(study)
            case Some(chapter) =>
              val fixed = study.withChapter(chapter)
              studyRepo.updateSomeFields(fixed).inject(Study.WithChapter(fixed, chapter).some)
          }
        case Some(chapter) => fuccess(Study.WithChapter(study, chapter).some)
      }

  def byIdWithChapter(id: StudyId, chapterId: StudyChapterId): Fu[Option[Study.WithChapter]] =
    studyRepo.byIdWithChapter(chapterRepo.coll)(id, chapterId)

  def byIdWithChapterOrFallback(id: StudyId, chapterId: StudyChapterId): Fu[Option[Study.WithChapter]] =
    byIdWithChapter(id, chapterId).orElse(byIdWithChapter(id))

  def byIdWithFirstChapter(id: StudyId): Fu[Option[Study.WithChapter]] =
    byIdWithChapterFinder(id, chapterRepo.firstByStudy(id))

  def byChapterId(chapterId: StudyChapterId): Fu[Option[Study.WithChapter]] =
    chapterRepo.byId(chapterId).flatMapz { chapter =>
      studyRepo.byId(chapter.studyId).mapz { Study.WithChapter(_, chapter).some }
    }

  private[study] def byIdWithLastChapter(id: StudyId): Fu[Option[Study.WithChapter]] =
    byIdWithChapterFinder(id, chapterRepo.lastByStudy(id))

  private def byIdWithChapterFinder(
      id: StudyId,
      chapterFinder: => Fu[Option[Chapter]]
  ): Fu[Option[Study.WithChapter]] =
    byId(id).flatMapz: study =>
      chapterFinder
        .mapz {
          Study.WithChapter(study, _).some
        }
        .orElse(byIdWithChapter(id))

  private def fixNoChapter(study: Study): Fu[Option[Study.WithChapter]] =
    sequenceStudy(study.id) { study =>
      chapterRepo.existsByStudy(study.id).flatMap {
        if _ then funit
        else
          for
            chap <- chapterMaker
              .fromFenOrPgnOrBlank(
                study,
                ChapterMaker.Data(StudyChapterName("Chapter 1")),
                order = 1,
                userId = study.ownerId
              )
            _ <- chapterRepo.insert(chap)
          yield preview.invalidate(study.id)
      }
    } >> byIdWithFirstChapter(study.id)

  def recentByOwnerWithChapterCount       = studyRepo.recentByOwnerWithChapterCount(chapterRepo.coll)
  def recentByContributorWithChapterCount = studyRepo.recentByContributorWithChapterCount(chapterRepo.coll)

  export chapterRepo.studyIdOf

  def members(id: StudyId): Fu[Option[StudyMembers]] = studyRepo.membersById(id)

  def importGame(
      data: StudyMaker.ImportGame,
      user: User,
      withRatings: Boolean
  ): Fu[Option[Study.WithChapter]] = data.form.as match
    case StudyForm.importGame.As.NewStudy =>
      create(data, user, withRatings).addEffect:
        _.so: sc =>
          Bus.publish(hub.StartStudy(sc.study.id), "startStudy")
    case StudyForm.importGame.As.ChapterOf(studyId) =>
      byId(studyId)
        .flatMap:
          case Some(study) if study.canContribute(user.id) =>
            addChapter(
              studyId = study.id,
              data = data.form.toChapterData,
              sticky = study.settings.sticky,
              withRatings
            )(Who(user.id, Sri(""))) >> byIdWithLastChapter(studyId)
          case _ => fuccess(none)
        .orElse(importGame(data.copy(form = data.form.copy(asStr = none)), user, withRatings))

  def create(
      data: StudyMaker.ImportGame,
      user: User,
      withRatings: Boolean,
      transform: Study => Study = identity
  ): Fu[Option[Study.WithChapter]] = for
    pre <- studyMaker(data, user, withRatings)
    sc = pre.copy(study = transform(pre.study))
    _ <- studyRepo.insert(sc.study)
    _ <- chapterRepo.insert(sc.chapter)
  yield
    indexStudy(sc.study)
    sc.some

  def cloneWithChat(me: User, prev: Study, update: Study => Study = identity): Fu[Option[Study]] = for
    study <- justCloneNoChecks(me, prev, update)
    _     <- chatApi.system(study.id.into(ChatId), s"Cloned from lichess.org/study/${prev.id}", _.study)
  yield study.some

  def justCloneNoChecks(
      me: User,
      prev: Study,
      update: Study => Study = identity
  ): Fu[Study] =
    val study1 = update(prev.cloneFor(me))
    chapterRepo
      .orderedByStudySource(prev.id)
      .map(_.cloneFor(study1))
      .mapAsync(1): c =>
        chapterRepo.insert(c).inject(c)
      .toMat(Sink.reduce[Chapter] { (prev, _) => prev })(Keep.right)
      .run()
      .flatMap: first =>
        val study = study1.rewindTo(first.id)
        studyRepo.insert(study).inject(study)

  export preview.dataList.{ apply as chapterPreviews }

  def maybeResetAndGetChapterPreviews(
      study: Study,
      chapter: Chapter
  ): Fu[(Study, Chapter, ChapterPreview.AsJsons)] =
    preview
      .jsonList(study.id)
      .flatMap: previews =>
        val defaultResult = (study, chapter, previews)
        if study.isRelay || !study.isOld || study.position == chapter.initialPosition
        then fuccess(defaultResult)
        else
          ChapterPreview.json.readFirstId(previews) match
            case Some(firstId) =>
              val newStudy = study.rewindTo(firstId)
              if newStudy == study then fuccess(defaultResult)
              else
                logger.info(s"Reset study ${study.id} to chapter $firstId")
                studyRepo
                  .updateSomeFields(newStudy)
                  .zip(chapterRepo.byId(firstId))
                  .map: (_, newChapter) =>
                    (newStudy, newChapter | chapter, previews)
            case None =>
              logger.warn(s"Couldn't reset study ${study.id}, no first chapter id found?!")
              fuccess(defaultResult)

  def talk(userId: UserId, studyId: StudyId, text: String) =
    byId(studyId).foreach:
      _.foreach: study =>
        (study.canChat(userId)).so {
          chatApi.write(
            study.id.into(ChatId),
            userId = userId,
            text = text,
            publicSource = lila.core.shutup.PublicSource.Study(studyId).some,
            busChan = _.study
          )
        }

  def setPath(studyId: StudyId, position: Position.Ref)(who: Who): Funit =
    sequenceStudy(studyId): study =>
      Contribute(who.u, study):
        chapterRepo
          .byId(position.chapterId)
          .map:
            _.filter: c =>
              c.root.pathExists(position.path) && study.position.chapterId == c.id
          .flatMap:
            case None => funit.andDo(sendTo(study.id)(_.reloadSri(who.sri)))
            case Some(chapter) if study.position.path != position.path =>
              (studyRepo.setPosition(study.id, position) >>
                updateConceal(study, chapter, position)).andDo(sendTo(study.id)(_.setPath(position, who)))
            case _ => funit

  def addNode(
      studyId: StudyId,
      position: Position.Ref,
      node: Branch,
      opts: MoveOpts,
      relay: Option[Chapter.Relay] = None
  )(who: Who): Funit =
    sequenceStudyWithChapter(studyId, position.chapterId):
      case Study.WithChapter(study, chapter) =>
        Contribute(who.u, study):
          doAddNode(study, Position(chapter, position.path), node, opts, relay)(who)
    .flatMapz { _() }

  private def doAddNode(
      study: Study,
      position: Position,
      rawNode: Branch,
      opts: MoveOpts,
      relay: Option[Chapter.Relay]
  )(who: Who): Fu[Option[() => Funit]] =
    val singleNode   = rawNode.withoutChildren
    def failReload() = reloadSriBecauseOf(study, who.sri, position.chapter.id)
    if position.chapter.isOverweight then
      logger.info(s"Overweight chapter ${study.id}/${position.chapter.id}")
      failReload()
      fuccess(none)
    else
      position.chapter.addNode(singleNode, position.path, relay) match
        case None =>
          failReload()
          fufail(s"Invalid addNode ${study.id} ${position.ref} $singleNode")
        case Some(chapter) =>
          chapter.root.nodeAt(position.path).so { parent =>
            parent.children.get(singleNode.id).so { node =>
              val newPosition = position.ref + node
              for
                _ <- chapterRepo.addSubTree(chapter, node, position.path, relay)
                _ <-
                  if opts.sticky
                  then studyRepo.setPosition(study.id, newPosition)
                  else studyRepo.updateNow(study)
                _ <- updateConceal(study, chapter, newPosition)
                _ = sendTo(study.id):
                  _.addNode(position.ref, node, chapter.setup.variant, sticky = opts.sticky, relay, who)
                promoteToMainline = opts.promoteToMainline && !newPosition.path.isMainline(chapter.root)
              yield promoteToMainline.option: () =>
                promote(study.id, position.ref + node, toMainline = true)(who)
            }
          }

  private def updateConceal(study: Study, chapter: Chapter, position: Position.Ref) =
    chapter.conceal.so: conceal =>
      chapter.root.lastMainlinePlyOf(position.path).some.filter(_ > conceal).so { newConceal =>
        if newConceal >= chapter.root.lastMainlinePly then
          chapterRepo.removeConceal(chapter.id).andDo(sendTo(study.id)(_.setConceal(position, none)))
        else
          chapterRepo
            .setConceal(chapter.id, newConceal)
            .andDo(sendTo(study.id)(_.setConceal(position, newConceal.some)))
      }

  def deleteNodeAt(studyId: StudyId, position: Position.Ref)(who: Who) =
    sequenceStudyWithChapter(studyId, position.chapterId):
      case Study.WithChapter(study, chapter) =>
        Contribute(who.u, study):
          chapter.updateRoot { root =>
            root.withChildren(_.deleteNodeAt(position.path))
          } match
            case Some(newChapter) =>
              chapterRepo.update(newChapter).andDo(sendTo(study.id)(_.deleteNode(position, who)))
            case None =>
              fufail(s"Invalid delNode $studyId $position").andDo(
                reloadSriBecauseOf(study, who.sri, chapter.id)
              )

  def resetRoot(studyId: StudyId, chapterId: StudyChapterId, newRoot: lila.tree.Root)(who: Who) =
    sequenceStudyWithChapter(studyId, chapterId):
      case Study.WithChapter(study, prevChapter) =>
        val chapter = prevChapter.copy(root = newRoot)
        chapterRepo
          .update(chapter)
          .andDo(sendTo(study.id)(_.updateChapter(chapter.id, who)))
          .inject(chapter.some)

  def clearAnnotations(studyId: StudyId, chapterId: StudyChapterId)(who: Who) =
    sequenceStudyWithChapter(studyId, chapterId):
      case Study.WithChapter(study, chapter) =>
        Contribute(who.u, study):
          chapterRepo
            .update(chapter.updateRoot { root =>
              root.withChildren(_.updateAllWith(_.clearAnnotations).some)
            } | chapter)
            .andDo(sendTo(study.id)(_.updateChapter(chapter.id, who)))

  def clearVariations(studyId: StudyId, chapterId: StudyChapterId)(who: Who) =
    sequenceStudyWithChapter(studyId, chapterId):
      case Study.WithChapter(study, chapter) =>
        Contribute(who.u, study):
          chapterRepo
            .update(chapter.copy(root = chapter.root.clearVariations))
            .andDo(sendTo(study.id)(_.updateChapter(chapter.id, who)))

  // rewrites the whole chapter because of `forceVariation`. Very inefficient.
  def promote(studyId: StudyId, position: Position.Ref, toMainline: Boolean)(who: Who): Funit =
    sequenceStudyWithChapter(studyId, position.chapterId):
      case Study.WithChapter(study, chapter) =>
        Contribute(who.u, study):
          chapter
            .updateRoot:
              _.withChildren: children =>
                if toMainline then children.promoteToMainlineAt(position.path)
                else children.promoteUpAt(position.path).map(_._1)
            .match
              case Some(newChapter) =>
                chapterRepo.update(newChapter) >>
                  newChapter.root.children
                    .nodesOn:
                      newChapter.root.mainlinePath.intersect(position.path)
                    .collect:
                      case (node, path) if node.forceVariation =>
                        doForceVariation(Study.WithChapter(study, newChapter), path, force = false, who)
                    .parallel
                    .map: _ =>
                      sendTo(study.id)(_.promote(position, toMainline, who))
              case None =>
                fufail(s"Invalid promoteToMainline $studyId $position").andDo(
                  reloadSriBecauseOf(study, who.sri, chapter.id)
                )

  def forceVariation(studyId: StudyId, position: Position.Ref, force: Boolean)(who: Who): Funit =
    sequenceStudyWithChapter(studyId, position.chapterId): sc =>
      Contribute(who.u, sc.study):
        doForceVariation(sc, position.path, force, who)

  private def doForceVariation(sc: Study.WithChapter, path: UciPath, force: Boolean, who: Who): Funit =
    sc.chapter.forceVariation(force, path) match
      case Some(newChapter) =>
        chapterRepo
          .forceVariation(force)(newChapter, path)
          .andDo(sendTo(sc.study.id)(_.forceVariation(Position(newChapter, path).ref, force, who)))
      case None =>
        fufail(s"Invalid forceVariation ${Position(sc.chapter, path)} $force")
          .andDo(reloadSriBecauseOf(sc.study, who.sri, sc.chapter.id))

  def setRole(studyId: StudyId, userId: UserId, roleStr: String)(who: Who) =
    sequenceStudy(studyId): study =>
      canActAsOwner(study, who.u).flatMapz:
        val role    = StudyMember.Role.byId.getOrElse(roleStr, StudyMember.Role.Read)
        val members = study.members.update(userId, _.copy(role = role))
        studyRepo.setRole(study, userId, role).andDo(onMembersChange(study, members, members.ids))

  def invite(
      byUserId: UserId,
      studyId: StudyId,
      username: UserStr,
      isPresent: UserId => Fu[Boolean],
      onError: String => Unit
  ) =
    sequenceStudy(studyId): study =>
      inviter(byUserId, study, username, isPresent)
        .addEffects(
          err => onError(err.getMessage),
          user =>
            val members = study.members + StudyMember.make(user)
            onMembersChange(study, members, members.ids)
        )
        .void

  def kick(studyId: StudyId, userId: UserId, who: MyId) =
    sequenceStudy(studyId): study =>
      studyRepo
        .isAdminMember(study, who)
        .flatMap: isAdmin =>
          val allowed = study.isMember(userId) && {
            (isAdmin && !study.isOwner(userId)) || (study.isOwner(who) ^ (who.is(userId)))
          }
          allowed.so:
            studyRepo
              .removeMember(study, userId)
              .andDo(onMembersChange(study, (study.members - userId), study.members.ids))

  export studyRepo.{ isMember, isContributor }

  private def onMembersChange(
      study: Study,
      members: StudyMembers,
      sendToUserIds: Iterable[UserId]
  ): Unit =
    sendTo(study.id)(_.reloadMembers(members, sendToUserIds))
    indexStudy(study)

  def setShapes(studyId: StudyId, position: Position.Ref, shapes: Shapes)(who: Who) =
    sequenceStudy(studyId): study =>
      Contribute(who.u, study):
        chapterRepo
          .byIdAndStudy(position.chapterId, study.id)
          .flatMapz: chapter =>
            chapter.setShapes(shapes, position.path) match
              case Some(newChapter) =>
                studyRepo.updateNow(study)
                chapterRepo
                  .setShapes(shapes)(newChapter, position.path)
                  .andDo(sendTo(study.id)(_.setShapes(position, shapes, who)))
              case None =>
                fufail(s"Invalid setShapes $position $shapes").andDo(
                  reloadSriBecauseOf(study, who.sri, chapter.id)
                )

  def setClock(studyId: StudyId, position: Position.Ref, clock: Centis)(who: Who): Funit =
    sequenceStudyWithChapter(studyId, position.chapterId):
      doSetClock(_, position, clock)(who)

  private def doSetClock(sc: Study.WithChapter, position: Position.Ref, clock: Centis)(
      who: Who
  ): Funit =
    sc.chapter.setClock(clock.some, position.path) match
      case Some(chapter, newCurrentClocks) =>
        studyRepo.updateNow(sc.study)
        chapterRepo
          .setClockAndDenorm(chapter, position.path, clock, newCurrentClocks)
          .andDo:
            sendTo(sc.study.id)(_.setClock(position, clock.some, newCurrentClocks))
      case None =>
        fufail(s"Invalid setClock $position $clock").andDo:
          reloadSriBecauseOf(sc.study, who.sri, position.chapterId)

  def setTag(studyId: StudyId, setTag: actorApi.SetTag)(who: Who) =
    sequenceStudyWithChapter(studyId, setTag.chapterId):
      case Study.WithChapter(study, chapter) =>
        Contribute(who.u, study):
          doSetTags(study, chapter, PgnTags(chapter.tags + setTag.tag), who)

  def setTagsAndRename(
      studyId: StudyId,
      chapterId: StudyChapterId,
      tags: Tags,
      newName: Option[StudyChapterName]
  )(who: Who) =
    sequenceStudyWithChapter(studyId, chapterId):
      case Study.WithChapter(study, chapter) =>
        Contribute(who.u, study):
          for
            _ <- newName.so(chapterRepo.setName(chapterId, _))
            _ <- doSetTags(study, chapter, tags, who)
          yield ()

  private def doSetTags(study: Study, oldChapter: Chapter, tags: Tags, who: Who): Funit =
    val chapter = oldChapter.copy(tags = tags)
    (chapter.tags != oldChapter.tags)
      .so {
        (chapterRepo.setTagsFor(chapter) >> {
          PgnTags.setRootClockFromTags(chapter).so { c =>
            c.root.clock.so: clock =>
              doSetClock(Study.WithChapter(study, c), Position(c, UciPath.root).ref, clock)(who)
          }
        }).andDo(sendTo(study.id)(_.setTags(chapter.id, chapter.tags, who)))
      }
      .andDo(indexStudy(study))

  def setComment(studyId: StudyId, position: Position.Ref, text: Comment.Text)(who: Who) =
    sequenceStudyWithChapter(studyId, position.chapterId):
      case Study.WithChapter(study, chapter) =>
        Contribute(who.u, study):
          lightUserApi
            .async(who.u)
            .flatMapz: author =>
              val comment = Comment(
                id = Comment.Id.make,
                text = text,
                by = Comment.Author.User(author.id, author.titleName)
              )
              doSetComment(study, Position(chapter, position.path), comment, who)

  private def doSetComment(study: Study, position: Position, comment: Comment, who: Who): Funit =
    position.chapter.setComment(comment, position.path) match
      case Some(newChapter) =>
        studyRepo.updateNow(study)
        newChapter.root.nodeAt(position.path).so { node =>
          node.comments.findBy(comment.by).so { c =>
            chapterRepo.setComments(node.comments.filterEmpty)(newChapter, position.path).andDo {
              sendTo(study.id)(_.setComment(position.ref, c, who))
              indexStudy(study)
            }
          }
        }
      case None =>
        fufail(s"Invalid setComment ${study.id} $position")
          .andDo(reloadSriBecauseOf(study, who.sri, position.chapter.id))

  def deleteComment(studyId: StudyId, position: Position.Ref, id: Comment.Id)(who: Who) =
    sequenceStudyWithChapter(studyId, position.chapterId):
      case Study.WithChapter(study, chapter) =>
        Contribute(who.u, study):
          chapter.deleteComment(id, position.path) match
            case Some(newChapter) =>
              chapterRepo
                .update(newChapter)
                .andDo(sendTo(study.id)(_.deleteComment(position, id, who)))
                .andDo(indexStudy(study))
            case None =>
              fufail(s"Invalid deleteComment $studyId $position $id").andDo(
                reloadSriBecauseOf(study, who.sri, chapter.id)
              )

  def toggleGlyph(studyId: StudyId, position: Position.Ref, glyph: Glyph)(who: Who) =
    sequenceStudyWithChapter(studyId, position.chapterId):
      case Study.WithChapter(study, chapter) =>
        Contribute(who.u, study):
          chapter.toggleGlyph(glyph, position.path) match
            case Some(newChapter) =>
              studyRepo.updateNow(study)
              newChapter.root.nodeAt(position.path).so { node =>
                chapterRepo
                  .setGlyphs(node.glyphs)(newChapter, position.path)
                  .andDo(newChapter.root.nodeAt(position.path).foreach { node =>
                    sendTo(study.id)(_.setGlyphs(position, node.glyphs, who))
                  })
              }
            case None =>
              fufail(s"Invalid toggleGlyph $studyId $position $glyph").andDo(
                reloadSriBecauseOf(study, who.sri, chapter.id)
              )

  def setGamebook(studyId: StudyId, position: Position.Ref, gamebook: Gamebook)(who: Who) =
    sequenceStudyWithChapter(studyId, position.chapterId):
      case Study.WithChapter(study, chapter) =>
        Contribute(who.u, study):
          chapter.setGamebook(gamebook, position.path) match
            case Some(newChapter) =>
              studyRepo.updateNow(study)
              chapterRepo.setGamebook(gamebook)(newChapter, position.path).andDo(indexStudy(study))
            case None =>
              fufail(s"Invalid setGamebook $studyId $position").andDo(
                reloadSriBecauseOf(study, who.sri, chapter.id)
              )

  def explorerGame(studyId: StudyId, data: actorApi.ExplorerGame)(who: Who) =
    sequenceStudyWithChapter(studyId, data.position.chapterId):
      case Study.WithChapter(study, chapter) =>
        Contribute(who.u, study):
          if data.insert then
            explorerGameHandler.insert(study, Position(chapter, data.position.path), data.gameId).flatMap {
              case None =>
                fufail(s"Invalid explorerGame insert $studyId $data")
                  .andDo(reloadSriBecauseOf(study, who.sri, chapter.id))
              case Some((chapter, path)) =>
                studyRepo.updateNow(study)
                chapter.root.nodeAt(path).so { parent =>
                  chapterRepo.setChildren(parent.children)(chapter, path).andDo(sendTo(study.id)(_.reloadAll))
                }
            }
          else
            explorerGameHandler.quote(data.gameId).flatMapz {
              doSetComment(study, Position(chapter, data.position.path), _, who)
            }

  def addChapter(studyId: StudyId, data: ChapterMaker.Data, sticky: Boolean, withRatings: Boolean)(
      who: Who
  ): Fu[List[Chapter]] =
    data.manyGames match
      case Some(datas) =>
        datas.sequentially(addChapter(studyId, _, sticky, withRatings)(who)).map(_.flatten)
      case _ =>
        sequenceStudy(studyId): study =>
          Contribute(who.u, study):
            chapterRepo
              .countByStudyId(study.id)
              .flatMap: count =>
                if Study.maxChapters <= count then fuccess(Nil)
                else
                  for
                    _ <- data.initial.so:
                      chapterRepo
                        .firstByStudy(study.id)
                        .flatMap:
                          _.filter(_.isEmptyInitial).so(chapterRepo.delete)
                    order   <- chapterRepo.nextOrderByStudy(study.id)
                    chapter <- chapterMaker(study, data, order, who.u, withRatings)
                    _       <- doAddChapter(study, chapter, sticky, who)
                  yield List(chapter)
              .recover:
                case ChapterMaker.ValidationException(error) =>
                  sendTo(study.id)(_.validationError(error, who.sri))
                  Nil
              .addFailureEffect:
                case u => logger.error(s"StudyApi.addChapter to $studyId", u)

  def rename(studyId: StudyId, name: StudyName): Funit =
    sequenceStudy(studyId): old =>
      val study = old.copy(name = name)
      studyRepo.updateSomeFields(study).andDo(indexStudy(study))

  def importPgns(studyId: StudyId, datas: List[ChapterMaker.Data], sticky: Boolean, withRatings: Boolean)(
      who: Who
  ): Future[List[Chapter]] = datas
    .sequentially:
      addChapter(studyId, _, sticky, withRatings)(who)
    .map(_.flatten)

  def doAddChapter(study: Study, chapter: Chapter, sticky: Boolean, who: Who): Funit = for
    _ <- chapterRepo.insert(chapter)
    newStudy = study.withChapter(chapter)
    _ <- if sticky then studyRepo.updateSomeFields(newStudy) else studyRepo.updateNow(study)
  yield
    sendTo(study.id)(_.addChapter(newStudy.position, sticky, who))
    indexStudy(study)

  def setChapter(studyId: StudyId, chapterId: StudyChapterId)(who: Who) =
    sequenceStudy(studyId): study =>
      study.canContribute(who.u).so(doSetChapter(study, chapterId, who))

  private def doSetChapter(study: Study, chapterId: StudyChapterId, who: Who) =
    (study.position.chapterId != chapterId).so {
      chapterRepo.byIdAndStudy(chapterId, study.id).flatMapz { chapter =>
        val newStudy = study.withChapter(chapter)
        studyRepo.updateSomeFields(newStudy).andDo(sendTo(study.id)(_.changeChapter(newStudy.position, who)))
      }
    }

  def editChapter(studyId: StudyId, data: ChapterMaker.EditData)(who: Who) =
    sequenceStudy(studyId): study =>
      Contribute(who.u, study):
        chapterRepo.byIdAndStudy(data.id, studyId).flatMapz { chapter =>
          val name = Chapter.fixName(data.name)
          val newChapter = chapter.copy(
            name = name,
            practice = data.isPractice.option(true),
            gamebook = data.isGamebook.option(true),
            conceal = (chapter.conceal, data.isConceal) match
              case (None, true)     => chapter.root.ply.some
              case (Some(_), false) => None
              case _                => chapter.conceal
            ,
            setup = chapter.setup.copy(
              orientation = data.orientation match
                case ChapterMaker.Orientation.Fixed(color) => color
                case _                                     => chapter.setup.orientation
            ),
            description = data.hasDescription.option {
              chapter.description | "-"
            }
          )
          if chapter == newChapter then funit
          else
            chapterRepo.update(newChapter) >> {
              if chapter.conceal != newChapter.conceal then
                (newChapter.conceal.isDefined && study.position.chapterId == chapter.id)
                  .so {
                    val newPosition = study.position.withPath(UciPath.root)
                    studyRepo.setPosition(study.id, newPosition)
                  }
                  .andDo(sendTo(study.id)(_.reloadAll))
              else
                fuccess:
                  val shouldReload =
                    (newChapter.setup.orientation != chapter.setup.orientation) ||
                      (newChapter.practice != chapter.practice) ||
                      (newChapter.gamebook != chapter.gamebook) ||
                      (newChapter.description != chapter.description)
                  if shouldReload then sendTo(study.id)(_.updateChapter(chapter.id, who))
                  else reloadChapters(study)
            }
        }

  def descChapter(studyId: StudyId, data: ChapterMaker.DescData)(who: Who) =
    sequenceStudy(studyId): study =>
      Contribute(who.u, study):
        chapterRepo.byIdAndStudy(data.id, studyId).flatMapz { chapter =>
          val newChapter = chapter.copy(
            description = data.clean.nonEmpty.option(data.clean)
          )
          (chapter != newChapter).so:
            chapterRepo
              .update(newChapter)
              .andDo:
                sendTo(study.id)(_.descChapter(newChapter.id, newChapter.description, who))
                indexStudy(study)
        }

  def deleteChapter(studyId: StudyId, chapterId: StudyChapterId)(who: Who) =
    sequenceStudy(studyId): study =>
      Contribute(who.u, study):
        chapterRepo.byIdAndStudy(chapterId, studyId).flatMapz { chapter =>
          chapterRepo.idNames(studyId).flatMap { chaps =>
            // deleting the only chapter? Automatically create an empty one
            if chaps.sizeIs < 2 then
              chapterMaker(
                study,
                ChapterMaker.Data(StudyChapterName("Chapter 1")),
                1,
                who.u,
                withRatings = true
              ).flatMap: c =>
                doAddChapter(study, c, sticky = true, who) >> doSetChapter(study, c.id, who)
            // deleting the current chapter? Automatically move to another one
            else
              (study.position.chapterId == chapterId).so:
                chaps
                  .find(_.id != chapterId)
                  .so: newChap =>
                    doSetChapter(study, newChap.id, who)
          } >> chapterRepo.delete(chapter.id).andDo(reloadChapters(study))
        }

  def sortChapters(studyId: StudyId, chapterIds: List[StudyChapterId])(who: Who): Funit =
    sequenceStudy(studyId): study =>
      Contribute(who.u, study):
        chapterRepo.sort(study, chapterIds).andDo(reloadChapters(study))

  def descStudy(studyId: StudyId, desc: String)(who: Who) =
    sequenceStudy(studyId): study =>
      Contribute(who.u, study):
        val newStudy = study.copy(description = desc.nonEmpty.option(desc))
        (study != newStudy).so {
          studyRepo
            .updateSomeFields(newStudy)
            .andDo(sendTo(study.id)(_.descStudy(newStudy.description, who)))
            .andDo(indexStudy(study))
        }

  def setTopics(studyId: StudyId, topicStrs: List[String])(who: Who) =
    sequenceStudy(studyId): study =>
      Contribute(who.u, study):
        val topics    = StudyTopics.fromStrs(topicStrs, StudyTopics.studyMax)
        val newStudy  = study.copy(topics = topics.some)
        val newTopics = study.topics.fold(topics)(topics.diff)
        (study != newStudy).so {
          (studyRepo.updateTopics(newStudy) >>
            topicApi.userTopicsAdd(who.u, newTopics)).andDo {
            sendTo(study.id)(_.setTopics(topics, who))
            indexStudy(study)
            topicApi.recompute()
          }
        }

  def addTopics(studyId: StudyId, topics: List[String]) =
    sequenceStudy(studyId): study =>
      studyRepo.updateTopics(study.addTopics(StudyTopics.fromStrs(topics, StudyTopics.studyMax)))

  def editStudy(studyId: StudyId, data: Study.Data)(who: Who) =
    sequenceStudy(studyId): study =>
      canActAsOwner(study, who.u).flatMap: asOwner =>
        asOwner
          .option(data.settings)
          .so: settings =>
            val newStudy = study
              .copy(
                name = Study.toName(data.name),
                flair = data.flair.flatMap(flairApi.find),
                settings = settings,
                visibility = data.vis,
                description = settings.description.option:
                  study.description.filter(_.nonEmpty) | "-"
              )
            (newStudy != study).so:
              studyRepo
                .updateSomeFields(newStudy)
                .andDo(sendTo(study.id)(_.reloadAll))
                .andDo(indexStudy(study))

  def delete(study: Study) =
    sequenceStudy(study.id): study =>
      for
        _ <- studyRepo.delete(study)
        _ <- chapterRepo.deleteByStudy(study)
      yield Bus.publish(lila.core.study.RemoveStudy(study.id), "study")

  def deleteById(id: StudyId) =
    studyRepo.byId(id).flatMap(_.so(delete))

  def like(studyId: StudyId, v: Boolean)(who: Who): Funit =
    studyRepo.like(studyId, who.u, v).map { likes =>
      sendTo(studyId)(_.setLiking(Study.Liking(likes, v), who))
      if v then
        studyRepo.byId(studyId).foreach {
          _.filter(_.isPublic).foreach { study =>
            lila.common.Bus.pub(Propagate(StudyLike(who.u, study.id, study.name)).toFollowersOf(who.u))
          }
        }
    }

  def chapterIdNames(studyIds: List[StudyId]): Fu[Map[StudyId, Vector[Chapter.IdName]]] =
    chapterRepo.idNamesByStudyIds(studyIds, Study.maxChapters.value)

  def withLiked(me: Option[User])(studies: Seq[Study]): Fu[Seq[Study.WithLiked]] =
    me.so: u =>
      studyRepo.filterLiked(u, studies.map(_.id))
    .map: liked =>
        studies.map: study =>
          Study.WithLiked(study, liked(study.id))

  def analysisRequest(
      studyId: StudyId,
      chapterId: StudyChapterId,
      userId: UserId,
      official: Boolean = false
  ): Funit =
    sequenceStudyWithChapter(studyId, chapterId):
      case Study.WithChapter(study, chapter) =>
        Contribute(userId, study):
          serverEvalRequester(study, chapter, userId, official)

  def deleteAllChapters(studyId: StudyId, by: User) =
    sequenceStudy(studyId): study =>
      Contribute(by.id, study):
        chapterRepo.deleteByStudy(study).andDo(preview.invalidate(study.id))

  def becomeAdmin(studyId: StudyId, me: MyId): Funit =
    sequenceStudy(studyId)(inviter.becomeAdmin(me))

  private def indexStudy(study: Study) =
    Bus.publish(actorApi.SaveStudy(study), "study")

  private def reloadSriBecauseOf(study: Study, sri: Sri, chapterId: StudyChapterId) =
    sendTo(study.id)(_.reloadSriBecauseOf(sri, chapterId))

  def reloadChapters(study: Study) =
    preview
      .jsonList(study.id)
      .foreach: previews =>
        sendTo(study.id)(_.reloadChapters(previews))

  private def canActAsOwner(study: Study, userId: UserId): Fu[Boolean] =
    fuccess(study.isOwner(userId)) >>| studyRepo.isAdminMember(study, userId)

  private def Contribute[A](userId: UserId, study: Study)(f: => A)(using alleycats.Zero[A]): A =
    study.canContribute(userId).so(f)

  // work around circular dependency
  private var socket: Option[StudySocket]           = None
  private[study] def registerSocket(s: StudySocket) = socket = s.some
  private def sendTo(studyId: StudyId)(f: StudySocket => StudyId => Unit): Unit =
    socket.foreach: s =>
      f(s)(studyId)
