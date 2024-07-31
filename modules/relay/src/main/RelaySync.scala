package lila.relay

import chess.format.UciPath
import chess.format.pgn.{ Tag, Tags }

import lila.core.socket.Sri
import lila.study.*
import lila.tree.Branch

final private class RelaySync(
    studyApi: StudyApi,
    preview: ChapterPreviewApi,
    chapterRepo: ChapterRepo,
    tourRepo: RelayTourRepo,
    leaderboard: RelayLeaderboardApi,
    notifier: RelayNotifier
)(using Executor):

  def updateStudyChapters(rt: RelayRound.WithTour, rawGames: RelayGames): Fu[SyncResult.Ok] = for
    study    <- studyApi.byId(rt.round.studyId).orFail("Missing relay study!")
    chapters <- chapterRepo.orderedByStudyLoadingAllInMemory(study.id)
    games = RelayInputSanity.fixGames(rawGames)
    plan  = RelayUpdatePlan(chapters, games)
    _ <- plan.reorder.so(studyApi.sortChapters(study.id, _)(who(study.ownerId)))
    updates <- plan.update.sequentially: (chapter, game) =>
      updateChapter(rt, study, game, chapter)
    appends <- plan.append.toList.sequentially: game =>
      createChapter(rt, study, game)
    result = SyncResult.Ok(updates ::: appends.flatten, plan)
    _      = lila.common.Bus.publish(result, SyncResult.busChannel(rt.round.id))
    _ <- tourRepo.setSyncedNow(rt.tour)
  yield result

  private def updateChapter(
      rt: RelayRound.WithTour,
      study: Study,
      game: RelayGame,
      chapter: Chapter
  ): Fu[SyncResult.ChapterResult] = for
    chapter   <- updateInitialPosition(study.id, chapter, game)
    tagUpdate <- updateChapterTags(rt.tour, study, chapter, game)
    nbMoves   <- updateChapterTree(study, chapter, game)(using rt.tour)
    _         <- (nbMoves > 0).so(notifier.roundBegin(rt))
  yield SyncResult.ChapterResult(chapter.id, tagUpdate, nbMoves)

  private def createChapter(
      rt: RelayRound.WithTour,
      study: Study,
      game: RelayGame
  ): Fu[Option[SyncResult.ChapterResult]] =
    chapterRepo
      .countByStudyId(study.id)
      .flatMap: nb =>
        (RelayFetch.maxChapters > nb).so:
          createChapter(study, game)(using rt.tour).map: chapter =>
            SyncResult.ChapterResult(chapter.id, true, chapter.root.mainline.size).some

  private def updateInitialPosition(studyId: StudyId, chapter: Chapter, game: RelayGame): Fu[Chapter] =
    if game.root.mainline.sizeIs > 1 || game.root.fen == chapter.root.fen
    then fuccess(chapter)
    else
      studyApi
        .resetRoot(studyId, chapter.id, game.root.withoutChildren)(who(chapter.ownerId))
        .dmap(_ | chapter)

  private type NbMoves = Int
  private def updateChapterTree(study: Study, chapter: Chapter, game: RelayGame)(using
      RelayTour
  ): Fu[NbMoves] =
    val by = who(chapter.ownerId)
    val (path, newNode) = game.root.mainline.foldLeft(UciPath.root -> none[Branch]):
      case ((parentPath, None), gameNode) =>
        val path = parentPath + gameNode.id
        chapter.root.nodeAt(path) match
          case None => parentPath -> gameNode.some
          case Some(existing) =>
            gameNode.clock
              .filter(c => !existing.clock.has(c))
              .so: c =>
                studyApi.setClock(
                  studyId = study.id,
                  position = Position(chapter, path).ref,
                  clock = c
                )(by)
            path -> none
      case (found, _) => found
    for
      _ <- (chapter.root.children.nonEmpty && !path.isMainline(chapter.root)).so {
        logger.info(s"Change mainline ${showSC(study, chapter)} $path")
        studyApi.promote(
          studyId = study.id,
          position = Position(chapter, path).ref,
          toMainline = true
        )(by) >> chapterRepo.setRelayPath(chapter.id, path)
      }
      nbMoves <- newNode.so: node =>
        node.mainline
          .foldM(Position(chapter, path).ref): (position, n) =>
            val relay = Chapter.Relay(
              path = position.path + n.id,
              lastMoveAt = nowInstant,
              fideIds = game.fideIdsPair
            )
            studyApi
              .addNode(
                studyId = study.id,
                position = position,
                node = n,
                opts = moveOpts,
                relay = relay.some
              )(by)
              .inject(position + n)
          .inject:
            node.mainline.size
    yield nbMoves

  private def updateChapterTags(
      tour: RelayTour,
      study: Study,
      chapter: Chapter,
      game: RelayGame
  ): Fu[Boolean] =
    val gameTags = game.tags.value.foldLeft(Tags(Nil)): (newTags, tag) =>
      if !chapter.tags.value.has(tag) then newTags + tag
      else newTags
    val newEndTag = (
      game.outcome.isDefined &&
        gameTags(_.Result).isEmpty &&
        !chapter.tags(_.Result).has(game.showResult)
    ).option(Tag(_.Result, game.showResult))
    val tags = newEndTag.fold(gameTags)(gameTags + _)
    val chapterNewTags = tags.value.foldLeft(chapter.tags): (chapterTags, tag) =>
      PgnTags(chapterTags + tag)
    (chapterNewTags != chapter.tags).so {
      if vs(chapterNewTags) != vs(chapter.tags) then
        logger.info(s"Update ${showSC(study, chapter)} tags '${vs(chapter.tags)}' -> '${vs(chapterNewTags)}'")
      val newName = Chapter.nameFromPlayerTags(game.tags)
      for
        _ <- studyApi.setTagsAndRename(
          studyId = study.id,
          chapterId = chapter.id,
          tags = chapterNewTags,
          newName = newName.filter(_ != chapter.name)
        )(who(chapter.ownerId))
        newEnd = chapter.tags.outcome.isEmpty && tags.outcome.isDefined
        _ <- newEnd.so(onChapterEnd(tour, study, chapter))
      yield true
    }

  private def onChapterEnd(tour: RelayTour, study: Study, chapter: Chapter): Funit = for
    _ <- chapterRepo.setRelayPath(chapter.id, UciPath.root)
    _ <- (tour.official && chapter.root.mainline.sizeIs > 10).so:
      studyApi.analysisRequest(
        studyId = study.id,
        chapterId = chapter.id,
        userId = study.ownerId,
        official = true
      )
  yield
    preview.invalidate(study.id)
    studyApi.reloadChapters(study)
    leaderboard.invalidate(tour.id)

  private def makeRelayFor(game: RelayGame, path: UciPath)(using tour: RelayTour) =
    Chapter.Relay(
      path = path,
      lastMoveAt = nowInstant,
      fideIds = tour.official.so(game.fideIdsPair)
    )

  private def chapterName(game: RelayGame, order: Chapter.Order): StudyChapterName =
    Chapter.nameFromPlayerTags(game.tags) | StudyChapterName(s"Board $order")

  private def createChapter(study: Study, game: RelayGame)(using RelayTour): Fu[Chapter] = for
    order <- chapterRepo.nextOrderByStudy(study.id)
    chapter = Chapter.make(
      studyId = study.id,
      name = chapterName(game, order),
      setup = Chapter.Setup(none, game.variant, Color.White),
      root = game.root,
      tags = game.tags,
      order = order,
      ownerId = study.ownerId,
      practice = false,
      gamebook = false,
      conceal = none,
      relay = makeRelayFor(game, game.root.mainlinePath).some
    )
    _ <- studyApi.doAddChapter(study, chapter, sticky = false, who(study.ownerId))
  yield
    preview.invalidate(study.id)
    chapter

  private val moveOpts = MoveOpts(
    write = true,
    sticky = false,
    promoteToMainline = true
  )

  private val sri                 = Sri("")
  private def who(userId: UserId) = actorApi.Who(userId, sri)

  private def vs(tags: Tags) = s"${tags(_.White) | "?"} - ${tags(_.Black) | "?"}"

  private def showSC(study: Study, chapter: Chapter) =
    s"#${study.id} ${chapter.name}"

sealed trait SyncResult:
  val reportKey: String
object SyncResult:
  case class Ok(chapters: List[ChapterResult], plan: RelayUpdatePlan.Plan) extends SyncResult:
    def nbMoves        = chapters.foldLeft(0)(_ + _.newMoves)
    def hasMovesOrTags = chapters.exists(c => c.newMoves > 0 || c.tagUpdate)
    val reportKey      = "ok"
  case object Timeout extends Exception with SyncResult with util.control.NoStackTrace:
    val reportKey           = "timeout"
    override def getMessage = "In progress..."
  case class Error(msg: String) extends SyncResult:
    val reportKey = "error"

  case class ChapterResult(id: StudyChapterId, tagUpdate: Boolean, newMoves: Int)

  def busChannel(roundId: RelayRoundId) = s"relaySyncResult:$roundId"
