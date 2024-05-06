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

  def updateStudyChapters(rt: RelayRound.WithTour, games: RelayGames): Fu[SyncResult.Ok] = for
    study          <- studyApi.byId(rt.round.studyId).orFail("Missing relay study!")
    chapters       <- chapterRepo.orderedByStudyLoadingAllInMemory(study.id)
    sanitizedGames <- RelayInputSanity(chapters, games).fold(x => fufail(x.msg), fuccess)
    nbGames = sanitizedGames.size
    chapterUpdates <- sanitizedGames.traverse(createOrUpdateChapter(_, rt, study, chapters, nbGames))
    result = SyncResult.Ok(chapterUpdates.toList.flatten, games)
    _      = lila.common.Bus.publish(result, SyncResult.busChannel(rt.round.id))
    _ <- tourRepo.setSyncedNow(rt.tour)
  yield result

  private def createOrUpdateChapter(
      game: RelayGame,
      rt: RelayRound.WithTour,
      study: Study,
      chapters: List[Chapter],
      nbGames: Int
  ): Fu[Option[SyncResult.ChapterResult]] =
    findCorrespondingChapter(game, chapters, nbGames)
      .map(updateChapter(rt.tour, study, game, _).dmap(_.some))
      .getOrElse:
        chapterRepo
          .countByStudyId(study.id)
          .flatMap:
            case nb if RelayFetch.maxChapters <= nb => fuccess(none)
            case _ =>
              createChapter(study, game)(using rt.tour).flatMap: chapter =>
                chapters
                  .find(_.isEmptyInitial)
                  .ifTrue(chapter.order == 2)
                  .so { initial =>
                    studyApi.deleteChapter(study.id, initial.id)(who(study.ownerId))
                  }
                  .inject(
                    SyncResult
                      .ChapterResult(chapter.id, true, chapter.root.mainline.size)
                      .some
                  )
      .flatMapz: result =>
        ((result.newMoves > 0).so(notifier.roundBegin(rt))).inject(result.some)

  /* If push or single game, use the player tags (and site) to match with the study chapter.
   * Otherwise match using the game's multipgn index.
   *
   * So the TCEC style - one game per file, reusing the file for all games - is supported.
   * lichess will create a new chapter when the game player tags differ.
   */
  private def findCorrespondingChapter(
      game: RelayGame,
      chapters: List[Chapter],
      nbGames: Int
  ): Option[Chapter] =
    if game.isPush || nbGames == 1 || game.looksLikeLichess
    then chapters.find(c => game.staticTagsMatch(c.tags))
    else chapters.find(_.relay.exists(_.index == game.index))

  private def updateChapter(
      tour: RelayTour,
      study: Study,
      game: RelayGame,
      chapter: Chapter
  ): Fu[SyncResult.ChapterResult] = for
    chapter   <- updateInitialPosition(study.id, chapter, game)
    tagUpdate <- updateChapterTags(tour, study, chapter, game)
    nbMoves   <- updateChapterTree(study, chapter, game)(using tour)
  yield SyncResult.ChapterResult(chapter.id, tagUpdate, nbMoves)

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
      _ <- (!path.isMainline(chapter.root)).so {
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
              index = game.index,
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
            // if chapter.root.children.nodes.isEmpty && node.mainline.nonEmpty then
            //   studyApi.reloadChapters(study)
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
    val newEndTag = game.ending
      .ifFalse(gameTags(_.Result).isDefined)
      .filterNot(end => chapter.tags(_.Result).has(end.resultText))
      .map(end => Tag(_.Result, end.resultText))
    val tags = newEndTag.fold(gameTags)(gameTags + _)
    val chapterNewTags = tags.value.foldLeft(chapter.tags): (chapterTags, tag) =>
      PgnTags(chapterTags + tag)
    (chapterNewTags != chapter.tags).so {
      if vs(chapterNewTags) != vs(chapter.tags) then
        logger.info(s"Update ${showSC(study, chapter)} tags '${vs(chapter.tags)}' -> '${vs(chapterNewTags)}'")
      val newName = chapterName(game)
      for
        _ <- studyApi.setTagsAndRename(
          studyId = study.id,
          chapterId = chapter.id,
          tags = chapterNewTags,
          newName = Option.when(newName != chapter.name)(newName)
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
      index = game.index,
      path = path,
      lastMoveAt = nowInstant,
      fideIds = tour.official.so(game.fideIdsPair)
    )

  private def chapterName(game: RelayGame) = StudyChapterName:
    game.tags.names
      .mapN((w, b) => s"$w - $b")
      .orElse(game.tags("board"))
      .orElse(game.index.map(i => (i + 1).toString))
      .getOrElse("?")

  private def createChapter(study: Study, game: RelayGame)(using RelayTour): Fu[Chapter] = for
    order <- chapterRepo.nextOrderByStudy(study.id)
    chapter = Chapter.make(
      studyId = study.id,
      name = chapterName(game),
      setup = Chapter.Setup(
        none,
        game.variant,
        chess.Color.White
      ),
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
    s"#${study.id} chapter[${chapter.relay.fold("?")(_.index.toString)}]"

sealed trait SyncResult:
  val reportKey: String
object SyncResult:
  case class Ok(chapters: List[ChapterResult], games: RelayGames) extends SyncResult:
    def nbMoves   = chapters.foldLeft(0)(_ + _.newMoves)
    val reportKey = "ok"
  case object Timeout extends Exception with SyncResult with util.control.NoStackTrace:
    val reportKey           = "timeout"
    override def getMessage = "In progress..."
  case class Error(msg: String) extends SyncResult:
    val reportKey = "error"

  case class ChapterResult(id: StudyChapterId, tagUpdate: Boolean, newMoves: Int)

  def busChannel(roundId: RelayRoundId) = s"relaySyncResult:$roundId"
