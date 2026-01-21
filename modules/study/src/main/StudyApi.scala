package lila.study

import akka.stream.scaladsl.*
import chess.format.UciPath
import chess.format.pgn.{ Glyph, Tags, Comment as CommentStr }
import monocle.syntax.all.*
import alleycats.Zero

import lila.common.Bus
import lila.core.perm.Granter
import lila.core.socket.Sri
import lila.core.study as hub
import lila.core.timeline.{ Propagate, StudyLike }
import lila.core.data.ErrorMsg
import lila.tree.Clock
import lila.tree.Node.{ Comment, Gamebook, Shapes }
import cats.mtl.Handle.*

final class StudyApi(
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo,
    sequencer: StudySequencer,
    studyMaker: StudyMaker,
    chapterMaker: ChapterMaker,
    inviter: StudyInvite,
    explorerGameHandler: ExplorerGameApi,
    topicApi: StudyTopicApi,
    lightUserApi: lila.core.user.LightUserApi,
    chatApi: lila.core.chat.ChatApi,
    serverEvalRequester: ServerEval.Requester,
    preview: ChapterPreviewApi,
    flairApi: lila.core.user.FlairApi,
    userApi: lila.core.user.UserApi
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
      chapterRepo
        .byId(study.position.chapterId)
        .flatMap:
          case None =>
            chapterRepo
              .firstByStudy(study.id)
              .flatMap:
                case None => fixNoChapter(study)
                case Some(chapter) =>
                  val fixed = study.withChapter(chapter)
                  studyRepo.updateSomeFields(fixed).inject(Study.WithChapter(fixed, chapter).some)
          case Some(chapter) => fuccess(Study.WithChapter(study, chapter).some)

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
        .mapz(Study.WithChapter(study, _).some)
        .orElse(byIdWithChapter(id))

  private def fixNoChapter(study: Study): Fu[Option[Study.WithChapter]] =
    sequenceStudy(study.id) { study =>
      chapterRepo
        .existsByStudy(study.id)
        .flatMap:
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
    } >> byIdWithFirstChapter(study.id)

  def recentByOwnerWithChapterCount = studyRepo.recentByOwnerWithChapterCount(chapterRepo.coll)
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
          Bus.pub(hub.StartStudy(sc.study.id))
    case StudyForm.importGame.As.ChapterOf(studyId) =>
      byId(studyId)
        .flatMap:
          case Some(study) if study.canContribute(user.id) =>
            allow:
              addSingleChapter(
                studyId = study.id,
                data = data.form.toChapterData,
                sticky = study.settings.sticky,
                withRatings
              )(Who(user.id, Sri(""))) >> byIdWithLastChapter(studyId)
            .rescue: _ =>
              fuccess(none)
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
  yield sc.some

  def cloneWithChat(me: User, prev: Study, update: Study => Study = identity): Fu[Option[Study]] = for
    study <- justCloneNoChecks(me, prev, update)
    _ <- chatApi.system(study.id.into(ChatId), s"Cloned from lichess.org/study/${prev.id}", _.study)
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

  export preview.dataList.apply as chapterPreviews

  def maybeResetAndGetChapter(study: Study, chapter: Chapter): Fu[(Study, Chapter)] =
    val defaultResult = (study, chapter)
    if study.isRelay || !study.isOld || study.position == chapter.initialPosition
    then fuccess(defaultResult)
    else
      preview
        .dataList(study.id)
        .flatMap:
          _.headOption match
            case Some(first) =>
              val newStudy = study.rewindTo(first.id)
              if newStudy == study then fuccess(defaultResult)
              else
                studyRepo
                  .updateSomeFields(newStudy)
                  .zip(chapterRepo.byId(first.id))
                  .map: (_, newChapter) =>
                    (newStudy, newChapter | chapter)
            case None =>
              logger.warn(s"Couldn't reset study ${study.id}, no first chapter id found?!")
              fuccess(defaultResult)

  def talk(userId: UserId, studyId: StudyId, text: String) =
    byId(studyId).foreach:
      _.filter(_.canChat(userId)).foreach: study =>
        chatApi.write(
          study.id.into(ChatId),
          userId = userId,
          text = text,
          publicSource = study.isPublic.option(lila.core.chat.PublicSource.Study(studyId)),
          busChan = _.study
        )

  def setPath(studyId: StudyId, position: Position.Ref)(who: Who): Funit =
    sequenceStudy(studyId): study =>
      Contribute(who.u, study):
        chapterRepo
          .byId(position.chapterId)
          .map:
            _.filter: c =>
              c.root.pathExists(position.path) && study.position.chapterId == c.id
          .flatMap:
            case None => fuccess(sendTo(study.id)(_.reloadSri(who.sri)))
            case Some(_) if study.position.path != position.path =>
              for _ <- studyRepo.setPosition(study.id, position)
              yield sendTo(study.id)(_.setPath(position, who))
            case _ => funit

  def addNode(args: AddNode): Funit =
    import args.{ *, given }
    sequenceStudyWithChapter(studyId, positionRef.chapterId):
      case Study.WithChapter(study, chapter) =>
        Contribute(who.u, study):
          doAddNode(args, study, Position(chapter, positionRef.path))
    .flatMapz { _() }

  private def doAddNode(
      args: AddNode,
      study: Study,
      position: Position
  ): Fu[Option[() => Funit]] =
    import args.{ *, given }
    args
      .node(position.chapter.setup.variant)
      .map(_.withoutChildren)
      .fold(
        err => fufail(err.toString),
        node =>
          if node.ply >= Node.MAX_PLIES then fuccess(none)
          else if position.chapter.isOverweight then
            logger.info(s"Overweight chapter ${study.id}/${position.chapter.id}")
            reloadSriBecauseOf(study, who.sri, position.chapter.id, "overweight".some)
            fuccess(none)
          else
            position.chapter.addNode(node, position.path, relay) match
              case None =>
                reloadSriBecauseOf(study, who.sri, position.chapter.id)
                fufail(s"Invalid addNode ${study.id} ${position.ref} $node")
              case Some(chapter) =>
                chapter.root.nodeAt(position.path).so { parent =>
                  parent.children.get(node.id).so { node =>
                    val newPosition = position.ref + node
                    for
                      _ <- chapterRepo.addSubTree(chapter, node, position.path, relay)
                      _ <-
                        if opts.sticky
                        then studyRepo.setPosition(study.id, newPosition)
                        else studyRepo.updateNow(study)
                      _ = sendTo(study.id):
                        _.addNode(position.ref, node, chapter.setup.variant, sticky = opts.sticky, relay, who)
                      isMainline = newPosition.path.isMainline(chapter.root)
                      promoteToMainline = opts.promoteToMainline && !isMainline
                    yield promoteToMainline.option: () =>
                      promote(study.id, position.ref + node, toMainline = true)
                  }
                }
      )

  def deleteNodeAt(studyId: StudyId, position: Position.Ref)(who: Who) =
    sequenceStudyWithChapter(studyId, position.chapterId):
      case Study.WithChapter(study, chapter) =>
        Contribute(who.u, study):
          chapter.updateRoot { root =>
            root.withChildren(_.deleteNodeAt(position.path))
          } match
            case Some(newChapter) =>
              for _ <- chapterRepo.update(newChapter)
              yield
                sendTo(study.id)(_.deleteNode(position, who))
                studyRepo.updateNow(study)
            case None =>
              reloadSriBecauseOf(study, who.sri, chapter.id)
              fufail(s"Invalid delNode $studyId $position")

  def resetRoot(
      studyId: StudyId,
      chapterId: StudyChapterId,
      newRoot: lila.tree.Root,
      newVariant: chess.variant.Variant
  )(who: Who) =
    sequenceStudyWithChapter(studyId, chapterId):
      case Study.WithChapter(study, prevChapter) =>
        val chapter = prevChapter
          .copy(root = newRoot)
          .focus(_.setup.variant)
          .replace(newVariant)
        for
          _ <- chapterRepo.update(chapter)
          _ = onChapterChange(studyId, chapterId, who)
        yield chapter.some

  def clearAnnotations(studyId: StudyId, chapterId: StudyChapterId)(who: Who) =
    sequenceStudyWithChapter(studyId, chapterId):
      case Study.WithChapter(study, chapter) =>
        Contribute(who.u, study):
          val newChapter = chapter.updateRoot(_.clearAnnotationsRecursively.some) | chapter
          for _ <- chapterRepo.update(newChapter) yield onChapterChange(study.id, chapter.id, who)

  def clearVariations(studyId: StudyId, chapterId: StudyChapterId)(who: Who) =
    sequenceStudyWithChapter(studyId, chapterId):
      case Study.WithChapter(study, chapter) =>
        Contribute(who.u, study):
          for _ <- chapterRepo.update(chapter.copy(root = chapter.root.clearVariations))
          yield onChapterChange(study.id, chapter.id, who)

  // rewrites the whole chapter because of `forceVariation`. Very inefficient.
  def promote(studyId: StudyId, position: Position.Ref, toMainline: Boolean)(using who: Who): Funit =
    sequenceStudyWithChapter(studyId, position.chapterId):
      case Study.WithChapter(study, chapter) =>
        Contribute(who.u, study):
          chapter
            .updateRoot:
              _.withChildren: children =>
                if toMainline then children.promoteToMainlineAt(position.path)
                else children.promoteUpAt(position.path)._1F
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
                reloadSriBecauseOf(study, who.sri, chapter.id)
                fufail(s"Invalid promoteToMainline $studyId $position")

  def forceVariation(studyId: StudyId, position: Position.Ref, force: Boolean)(who: Who): Funit =
    sequenceStudyWithChapter(studyId, position.chapterId): sc =>
      Contribute(who.u, sc.study):
        doForceVariation(sc, position.path, force, who)

  private def doForceVariation(sc: Study.WithChapter, path: UciPath, force: Boolean, who: Who): Funit =
    sc.chapter.forceVariation(force, path) match
      case Some(newChapter) =>
        for _ <- chapterRepo.forceVariation(force)(newChapter, path)
        yield sendTo(sc.study.id)(_.forceVariation(Position(newChapter, path).ref, force, who))
      case None =>
        reloadSriBecauseOf(sc.study, who.sri, sc.chapter.id)
        fufail(s"Invalid forceVariation ${Position(sc.chapter, path)} $force")

  def setRole(studyId: StudyId, userId: UserId, roleStr: String)(who: Who) =
    sequenceStudy(studyId): study =>
      canActAsOwner(study, who.u).flatMapz:
        val role = StudyMember.Role.byId.getOrElse(roleStr, StudyMember.Role.Read)
        val members = study.members.update(userId, _.copy(role = role))
        for _ <- studyRepo.setRole(study, userId, role) yield onMembersChange(study, members, members.ids)

  def invite(
      byUserId: UserId,
      studyId: StudyId,
      username: UserStr,
      isPresent: UserId => Fu[Boolean]
  ) =
    sequenceStudy(studyId): study =>
      inviter(byUserId, study, username, isPresent).map: user =>
        val members = study.members + StudyMember.make(user)
        onMembersChange(study, members, members.ids)

  def kick(studyId: StudyId, userId: UserId, who: MyId) =
    sequenceStudy(studyId): study =>
      studyRepo
        .isAdminMember(study, who)
        .flatMap: isAdmin =>
          val allowed = study.isMember(userId) && {
            (isAdmin && !study.isOwner(userId)) || (study.isOwner(who) ^ (who.is(userId)))
          }
          allowed.so:
            for _ <- studyRepo.removeMember(study, userId)
            yield onMembersChange(study, (study.members - userId), study.members.ids)

  export studyRepo.{ isMember, isContributor }

  private def onChapterChange(id: StudyId, chapterId: StudyChapterId, who: Who) =
    sendTo(id)(_.updateChapter(chapterId, who))
    studyRepo.updateNow(id)

  private def onMembersChange(
      study: Study,
      members: StudyMembers,
      sendToUserIds: Iterable[UserId]
  ): Unit =
    sendTo(study.id)(_.reloadMembers(members, sendToUserIds))
    studyRepo.updateNow(study)
    Bus.pub(StudyMembers.OnChange(study))

  def setShapes(studyId: StudyId, position: Position.Ref, shapes: Shapes)(who: Who) =
    sequenceStudy(studyId): study =>
      Contribute(who.u, study):
        chapterRepo
          .byIdAndStudy(position.chapterId, study.id)
          .flatMapz: chapter =>
            chapter.setShapes(shapes, position.path) match
              case Some(newChapter) =>
                studyRepo.updateNow(study)
                for _ <- chapterRepo.setShapes(shapes)(newChapter, position.path)
                yield sendTo(study.id)(_.setShapes(position, shapes, who))
              case None =>
                reloadSriBecauseOf(study, who.sri, chapter.id)
                fufail(s"Invalid setShapes $position $shapes")

  def setClock(studyId: StudyId, position: Position.Ref, clock: Clock)(who: Who): Funit =
    sequenceStudyWithChapter(studyId, position.chapterId):
      doSetClock(_, position, clock)(who)

  private def doSetClock(sc: Study.WithChapter, position: Position.Ref, clock: Clock)(
      who: Who
  ): Funit =
    sc.chapter.setClock(clock.some, position.path) match
      case Some(chapter, newCurrentClocks) =>
        studyRepo.updateNow(sc.study)
        for _ <- chapterRepo.setClockAndDenorm(chapter, position.path, clock, newCurrentClocks)
        yield sendTo(sc.study.id)(_.setClock(position, clock.centis.some, newCurrentClocks))
      case None =>
        reloadSriBecauseOf(sc.study, who.sri, position.chapterId)
        fufail(s"Invalid setClock $position $clock")

  def setTag(studyId: StudyId, setTag: SetTag)(who: Who) =
    setTag.validate.so: tag =>
      sequenceStudyWithChapter(studyId, setTag.chapterId):
        case Study.WithChapter(study, chapter) =>
          Contribute(who.u, study):
            for _ <- doSetTags(study, chapter, StudyPgnTags(chapter.tags + tag), who)
            yield if study.isRelay then Bus.pub(AfterSetTagOnRelayChapter(setTag.chapterId, tag))

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
          yield studyRepo.updateNow(study)

  private def doSetTags(study: Study, oldChapter: Chapter, tags: Tags, who: Who): Funit =
    (tags != oldChapter.tags).so:
      val chapter = oldChapter.copy(tags = tags)
      for
        _ <- chapterRepo.setTagsFor(chapter)
        _ <- StudyPgnTags
          .setRootClockFromTags(chapter)
          .so: c =>
            c.root.clock.so: clock =>
              doSetClock(Study.WithChapter(study, c), Position(c, UciPath.root).ref, clock)(who)
      yield sendTo(study.id)(_.setTags(chapter.id, chapter.tags, who))

  def setComment(studyId: StudyId, position: Position.Ref, commentId: Option[Comment.Id], text: CommentStr)(
      who: Who
  ) =
    sequenceStudyWithChapter(studyId, position.chapterId):
      case Study.WithChapter(study, chapter) =>
        Contribute(who.u, study):
          lightUserApi
            .async(who.u)
            .flatMapz: author =>
              val comment = Comment(
                id = if commentId.getOrElse("") == "" then Comment.Id.make else commentId.get,
                text = text,
                by = Comment.Author.User(author.id, author.titleName)
              )
              doSetComment(study, Position(chapter, position.path), comment, who)

  private def doSetComment(study: Study, position: Position, comment: Comment, who: Who): Funit =
    position.chapter.setComment(comment, position.path) match
      case Some(newChapter) =>
        newChapter.root.nodeAt(position.path).so { node =>
          node.comments.findByIdAndAuthor(comment.id, comment.by).so { c =>
            for _ <- chapterRepo.setComments(node.comments.set(c))(newChapter, position.path)
            yield
              sendTo(study.id)(_.setComment(position.ref, c, who))
              studyRepo.updateNow(study)
          }
        }
      case None =>
        reloadSriBecauseOf(study, who.sri, position.chapter.id)
        fufail(s"Invalid setComment ${study.id} $position")

  def deleteComment(studyId: StudyId, position: Position.Ref, id: Comment.Id)(who: Who) =
    sequenceStudyWithChapter(studyId, position.chapterId):
      case Study.WithChapter(study, chapter) =>
        Contribute(who.u, study):
          chapter.deleteComment(id, position.path) match
            case Some(newChapter) =>
              for _ <- chapterRepo.update(newChapter)
              yield
                sendTo(study.id)(_.deleteComment(position, id, who))
                studyRepo.updateNow(study)
            case None =>
              reloadSriBecauseOf(study, who.sri, chapter.id)
              fufail(s"Invalid deleteComment $studyId $position $id")

  def toggleGlyph(studyId: StudyId, position: Position.Ref, glyph: Glyph)(who: Who) =
    sequenceStudyWithChapter(studyId, position.chapterId):
      case Study.WithChapter(study, chapter) =>
        Contribute(who.u, study):
          chapter.toggleGlyph(glyph, position.path) match
            case Some(newChapter) =>
              studyRepo.updateNow(study)
              newChapter.root.nodeAt(position.path).so { node =>
                for _ <- chapterRepo.setGlyphs(node.glyphs)(newChapter, position.path)
                yield newChapter.root.nodeAt(position.path).foreach { node =>
                  sendTo(study.id)(_.setGlyphs(position, node.glyphs, who))
                }
              }
            case None =>
              reloadSriBecauseOf(study, who.sri, chapter.id)
              fufail(s"Invalid toggleGlyph $studyId $position $glyph")

  def setGamebook(studyId: StudyId, position: Position.Ref, gamebook: Gamebook)(who: Who) =
    sequenceStudyWithChapter(studyId, position.chapterId):
      case Study.WithChapter(study, chapter) =>
        Contribute(who.u, study):
          chapter.setGamebook(gamebook, position.path) match
            case Some(newChapter) =>
              studyRepo.updateNow(study)
              chapterRepo.setGamebook(gamebook)(newChapter, position.path)
            case None =>
              reloadSriBecauseOf(study, who.sri, chapter.id)
              fufail(s"Invalid setGamebook $studyId $position")

  def explorerGame(studyId: StudyId, data: ExplorerGame)(who: Who) =
    sequenceStudyWithChapter(studyId, data.position.chapterId):
      case Study.WithChapter(study, chapter) =>
        Contribute(who.u, study):
          if data.insert then
            explorerGameHandler
              .insert(study, Position(chapter, data.position.path), data.gameId)
              .flatMap:
                case None =>
                  reloadSriBecauseOf(study, who.sri, chapter.id)
                  fufail(s"Invalid explorerGame insert $studyId $data")
                case Some(chapter, path) =>
                  studyRepo.updateNow(study)
                  chapter.root.nodeAt(path).so { parent =>
                    for _ <- chapterRepo.setChildren(parent.children)(chapter, path)
                    yield sendTo(study.id)(_.reloadAll)
                  }
          else
            explorerGameHandler
              .quote(data.gameId)
              .flatMapz:
                doSetComment(study, Position(chapter, data.position.path), _, who)

  def addChapter(studyId: StudyId, data: ChapterMaker.Data, sticky: Boolean, withRatings: Boolean)(
      who: Who
  ): FuRaise[ErrorMsg, List[Chapter]] =
    data.manyGames match
      case Some(datas) =>
        datas.sequentially(addSingleChapter(studyId, _, sticky, withRatings)(who)).map(_.flatten)
      case _ =>
        addSingleChapter(studyId, data, sticky, withRatings)(who).dmap(_.toList)

  def addSingleChapter(studyId: StudyId, data: ChapterMaker.Data, sticky: Boolean, withRatings: Boolean)(
      who: Who
  ): FuRaise[ErrorMsg, Option[Chapter]] =
    sequenceStudy(studyId): study =>
      for
        _ <- raiseIf(!study.canContribute(who.u))(ErrorMsg("No permission to add chapter"))
        count <- chapterRepo.countByStudyId(study.id)
        _ <- raiseIf(Study.maxChapters <= count)(ErrorMsg("Too many chapters"))
        _ <- data.initial.so:
          chapterRepo
            .firstByStudy(study.id)
            .flatMap:
              _.filter(_.isEmptyInitial).so(chapterRepo.delete)
        order <- chapterRepo.nextOrderByStudy(study.id)
        chapter <- chapterMaker(study, data, order, who.u, withRatings)
          .recoverWith:
            case ChapterMaker.ValidationException(error) =>
              sendTo(study.id)(_.validationError(error, who.sri))
              ErrorMsg(error).raise
        _ <- doAddChapter(study, chapter, sticky, who)
      yield chapter.some

  def rename(studyId: StudyId, name: StudyName): Funit =
    sequenceStudy(studyId): old =>
      val study = old.copy(name = name)
      studyRepo.updateSomeFields(study)

  def importPgns(studyId: StudyId, datas: List[ChapterMaker.Data], sticky: Boolean, withRatings: Boolean)(
      who: Who
  ): Future[(List[Chapter], Option[ErrorMsg])] =
    datas
      .sequentiallyRaise:
        addSingleChapter(studyId, _, sticky, withRatings)(who)
      .dmap: (oc, errors) =>
        (oc.flatten, errors)

  def doAddChapter(study: Study, chapter: Chapter, sticky: Boolean, who: Who): Funit =
    for
      _ <- chapterRepo.insert(chapter)
      newStudy = study.withChapter(chapter)
      _ <- if sticky then studyRepo.updateSomeFields(newStudy) else studyRepo.updateNow(study)
      _ = preview.invalidate(study.id)
    yield sendTo(study.id)(_.addChapter(newStudy.position, sticky, who))

  def setChapter(studyId: StudyId, chapterId: StudyChapterId)(who: Who) =
    sequenceStudy(studyId): study =>
      study.canContribute(who.u).so(doSetChapter(study, chapterId, who))

  private def doSetChapter(study: Study, chapterId: StudyChapterId, who: Who) =
    (study.position.chapterId != chapterId).so:
      chapterRepo.byIdAndStudy(chapterId, study.id).flatMapz { chapter =>
        val newStudy = study.withChapter(chapter)
        for _ <- studyRepo.updateSomeFields(newStudy)
        yield sendTo(study.id)(_.changeChapter(newStudy.position, who))
      }

  def editChapter(studyId: StudyId, data: ChapterMaker.EditData)(who: Who) =
    sequenceStudy(studyId): study =>
      Contribute(who.u, study):
        chapterRepo.byIdAndStudy(data.id, studyId).flatMapz { chapter =>
          val newChapter = chapter.copy(
            name = Chapter.fixName(data.name),
            practice = data.isPractice.option(true),
            gamebook = data.isGamebook.option(true),
            conceal = (chapter.conceal, data.isConceal) match
              case (None, true) => chapter.root.ply.some
              case (Some(_), false) => None
              case _ => chapter.conceal
            ,
            setup = chapter.setup.copy(
              orientation = data.orientation match
                case ChapterMaker.Orientation.Fixed(color) => color
                case _ => chapter.setup.orientation
            ),
            description = data.hasDescription.option {
              chapter.description | "-"
            }
          )
          (chapter != newChapter).so:
            for
              _ <- chapterRepo.update(newChapter)
              concealChanged = chapter.conceal != newChapter.conceal
              shouldResetPosition =
                concealChanged && newChapter.conceal.isDefined && study.position.chapterId == chapter.id
              shouldReload =
                concealChanged ||
                  newChapter.setup.orientation != chapter.setup.orientation ||
                  newChapter.practice != chapter.practice ||
                  newChapter.gamebook != chapter.gamebook ||
                  newChapter.description != chapter.description
              _ <- shouldResetPosition.so:
                studyRepo.setPosition(study.id, study.position.withPath(UciPath.root))
            yield
              if shouldReload // `updateChapter` makes the client reload the whole thing with XHR
              then sendTo(study.id)(_.updateChapter(chapter.id, who))
              else reloadChapters(study)
        }

  def descChapter(studyId: StudyId, data: ChapterMaker.DescData)(who: Who) =
    sequenceStudy(studyId): study =>
      Contribute(who.u, study):
        chapterRepo.byIdAndStudy(data.id, studyId).flatMapz { chapter =>
          val newChapter = chapter.copy(
            description = data.clean.nonEmpty.option(data.clean)
          )
          (chapter != newChapter).so:
            for _ <- chapterRepo.update(newChapter)
            yield sendTo(study.id)(_.descChapter(newChapter.id, newChapter.description, who))
        }

  def deleteChapter(studyId: StudyId, chapterId: StudyChapterId)(who: Who) =
    sequenceStudy(studyId): study =>
      Contribute(who.u, study):
        chapterRepo.byIdAndStudy(chapterId, studyId).flatMapz { chapter =>
          for
            chaps <- chapterRepo.idNames(studyId)
            // deleting the only chapter? Automatically create an empty one
            _ <-
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
                  val ids = chaps.map(_.id)
                  val i = ids.indexOf(chapterId)
                  val newIdOpt = LazyList(i + 1, i - 1, 0).flatMap(ids.lift).headOption
                  newIdOpt.so: newId =>
                    doSetChapter(study, newId, who)
            _ <- chapterRepo.delete(chapter.id)
          yield
            reloadChapters(study)
            studyRepo.updateNow(study)
        }

  // update provided tags, keep missing tags, delete tags with empty value
  def updateChapterTags(studyId: StudyId, chapterId: StudyChapterId, tags: Tags)(using me: Me) =
    sequenceStudyWithChapter(studyId, chapterId):
      case Study.WithChapter(study, chapter) =>
        Contribute(me, study):
          val newTags = tags.value.foldLeft(chapter.tags): (ctags, tag) =>
            if tag.value.isEmpty
            then ctags - tag.name
            else ctags + tag
          doSetTags(study, chapter, newTags, Who(me.userId, Sri("")))

  def sortChapters(studyId: StudyId, chapterIds: List[StudyChapterId])(who: Who): Funit =
    sequenceStudy(studyId): study =>
      Contribute(who.u, study):
        for _ <- chapterRepo.sort(study, chapterIds) yield reloadChapters(study)

  def descStudy(studyId: StudyId, desc: String)(who: Who) =
    sequenceStudy(studyId): study =>
      Contribute(who.u, study):
        val newStudy = study.copy(description = desc.nonEmpty.option(desc))
        (study != newStudy).so:
          for _ <- studyRepo.updateSomeFields(newStudy)
          yield sendTo(study.id)(_.descStudy(newStudy.description, who))

  def setTopics(studyId: StudyId, topicStrs: List[String])(who: Who) =
    sequenceStudy(studyId): study =>
      Contribute(who.u, study):
        val topics = StudyTopics.fromStrs(topicStrs, StudyTopics.studyMax)
        val newStudy = study.copy(topics = topics.some)
        val newTopics = study.topics.fold(topics)(topics.diff)
        (study != newStudy).so:
          for
            _ <- studyRepo.updateTopics(newStudy)
            _ <- topicApi.userTopicsAdd(who.u, newTopics)
          yield
            sendTo(study.id)(_.setTopics(topics, who))
            topicApi.recompute()

  def setVisibility(studyId: StudyId, visibility: hub.Visibility): Funit =
    sequenceStudy(studyId): study =>
      (study.visibility != visibility).so:
        for _ <- studyRepo.updateSomeFields(study.copy(visibility = visibility))
        yield sendTo(study.id)(_.reloadAll)

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
                visibility = data.visibility,
                description = settings.description.option:
                  study.description.filter(_.nonEmpty) | "-"
              )
            (newStudy != study).so:
              for _ <- studyRepo.updateSomeFields(newStudy)
              yield sendTo(study.id)(_.reloadAll)

  def delete(study: Study) =
    sequenceStudy(study.id): study =>
      for
        _ <- studyRepo.delete(study)
        _ <- chapterRepo.deleteByStudy(study)
      yield Bus.pub(lila.core.study.RemoveStudy(study.id))

  def deleteById(id: StudyId) =
    studyRepo.byId(id).flatMap(_.so(delete))

  def like(studyId: StudyId, v: Boolean)(who: Who): Funit =
    studyRepo.like(studyId, who.u, v).map { likes =>
      sendTo(studyId)(_.setLiking(Study.Liking(likes, v), who))
      if v then
        studyRepo
          .byId(studyId)
          .foreach:
            _.filter(_.isPublic).foreach { study =>
              lila.common.Bus.pub(Propagate(StudyLike(who.u, study.id, study.name)).toFollowersOf(who.u))
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
        for _ <- chapterRepo.deleteByStudy(study) yield preview.invalidate(study.id)

  def becomeAdmin(studyId: StudyId, me: MyId): Funit =
    sequenceStudy(studyId): study =>
      for _ <- inviter.becomeAdmin(me)(study)
      yield Bus.pub(StudyMembers.OnChange(study))

  private def reloadSriBecauseOf(
      study: Study,
      sri: Sri,
      chapterId: StudyChapterId,
      reason: Option["overweight"] = none
  ) =
    sendTo(study.id)(_.reloadSriBecauseOf(sri, chapterId, reason))

  def reloadChapters(study: Study) =
    preview
      .jsonList(study.id)
      .foreach: previews =>
        sendTo(study.id)(_.reloadChapters(previews))

  private def canActAsOwner(study: Study, userId: UserId): Fu[Boolean] =
    fuccess(study.isOwner(userId)) >>| studyRepo.isAdminMember(study, userId) >>|
      userApi.byId(userId).map(_.exists(Granter.ofUser(_.StudyAdmin)))

  private def Contribute[A: Zero](userId: UserId, study: Study)(f: => A): A =
    study.canContribute(userId).so(f)

  // work around circular dependency
  private var socket: Option[StudySocket] = None
  private[study] def registerSocket(s: StudySocket) = socket = s.some
  private def sendTo(studyId: StudyId)(f: StudySocket => StudyId => Unit): Unit =
    socket.foreach: s =>
      f(s)(studyId)
