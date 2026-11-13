package lila.relay

import chess.format.UciPath
import chess.format.pgn.{ Tag, Tags }

import lila.common.Bus
import lila.core.socket.Sri
import lila.study.*
import lila.tree.{ Branch, Node }

final private class RelaySync(
    studyApi: StudyApi,
    chapterRepo: ChapterRepo,
    tourRepo: RelayTourRepo,
    groupRepo: RelayGroupRepo,
    players: RelayPlayerApi,
    teamLeaderboard: RelayTeamLeaderboard,
    notifier: RelayNotifier,
    tagManualOverride: RelayTagManualOverride
)(using Executor)(using scheduler: Scheduler):

  def updateStudyChapters(rt: RelayRound.WithTour, rawGames: RelayGames): Fu[SyncResult.Ok] = for
    study <- studyApi.byId(rt.round.studyId).orFail("Missing relay study!")
    chapters <- chapterRepo.orderedByStudyLoadingAllInMemory(study.id)
    games = RelayInputSanity.fixGames(rawGames)
    plan = RelayUpdatePlan(chapters, games)
    _ <- plan.reorder.so(studyApi.sortChapters(study.id, _)(who(study.ownerId)))
    updates <- plan.update.sequentially: (chapter, game) =>
      updateChapter(rt, study, chapter, game)
    allowedNbChapters <- plan.append.nonEmpty.so:
      chapterRepo.countByStudyId(study.id).map(RelayFetch.maxChaptersToShow.value - _)
    appends <- plan.append.take(allowedNbChapters).toList.sequentially(createChapter(rt, study, _))
    groupId <- groupRepo.idByTour(rt.tour.id)
    result = SyncResult.Ok(updates ::: appends.flatten, plan, rt.withStudy(study))
    _ <- tourRepo.setSyncedNow(rt.tour)
    // because studies always have a chapter,
    // broadcasts without game have an empty initial chapter.
    // When a single game comes from the source, the initial chapter
    // is updated instead of created. The client might be confused.
    // So, send them all the chapter preview with `reloadChapters`
    reloadChapters = updates.exists(_.newEnd) || plan.isJustInitialChapterUpdate
    _ = if reloadChapters then
      studyApi.invalidateAndSendChapterPreviews(study)
      players.invalidate(rt.tour.id)
      teamLeaderboard.invalidate(rt.tour.id)
  yield
    Bus.publishDyn(result, SyncResult.roundBusChannel(rt.round.id))
    Bus.publishDyn(result, SyncResult.tourBusChannel(rt.tour.id))
    groupId.foreach(g => Bus.publishDyn(result, SyncResult.groupBusChannel(g)))
    result

  private def updateChapter(
      rt: RelayRound.WithTour,
      study: Study,
      chapter: Chapter,
      game: RelayGame
  ): Fu[SyncResult.ChapterResult] =
    val delaySeconds = rt.round.sync.delayMinusLag.fold(0)(_.value)
    for
      chapter <- updateInitialPosition(study.id, chapter, game)
      chapter <- ensureChapterRelayField(chapter, game, delaySeconds)(using rt.tour)
      staleSnapshot = RelayClock.isOlderSnapshot(chapter, game)
      (newTags, newEnd) <- updateChapterTags(study, chapter, game)
      updatedChapter = newTags.fold(chapter): tags =>
        val tagged = chapter.withTags(tags)
        if tagged.root.children.isEmpty then
          StudyPgnTags.setRootClockFromTags(tagged).filter(_.root.clock.isDefined) | tagged
        else tagged
      (nbMoves, clockUpdate) <- updateChapterTree(
        study,
        updatedChapter,
        game,
        delaySeconds,
        staleSnapshot,
        newEnd
      )(using rt.tour)
      _ <- (newEnd || (updatedChapter.tags.points.isDefined && nbMoves > 0))
        .so(onChapterEnd(rt.tour, study, chapter))
      _ = if nbMoves > 0 then notifier.onUpdate(rt, newTags.foldLeft(chapter)(_.withTags(_)))
    yield SyncResult.ChapterResult(chapter.id, newTags.isDefined, nbMoves, newEnd, clockUpdate)

  private def createChapter(
      rt: RelayRound.WithTour,
      study: Study,
      game: RelayGame
  ): Fu[Option[SyncResult.ChapterResult]] =
    chapterRepo
      .countByStudyId(study.id)
      .flatMap: nb =>
        (RelayFetch.maxChaptersToShow > nb).so:
          for
            chapter <- createChapter(study, game, rt.round.sync.delayMinusLag.fold(0)(_.value))(using rt.tour)
            _ <- chapter.tags.points.isDefined.so:
              onChapterEnd(rt.tour, study, chapter)
          yield
            if chapter.root.mainline.nonEmpty then notifier.onCreate(rt, chapter)
            SyncResult.ChapterResult(chapter.id, true, chapter.root.mainline.size, false).some

  private def updateInitialPosition(studyId: StudyId, chapter: Chapter, game: RelayGame): Fu[Chapter] =
    if chapter.root.mainline.sizeIs > 1 || game.root.fen == chapter.root.fen
    then fuccess(chapter)
    else
      studyApi
        .resetRoot(studyId, chapter.id, game.root.withoutChildren, game.variant)(who(chapter.ownerId))
        .dmap(_ | chapter)

  // because a study always has at least one chapter,
  // the first chapter is updated when the board data arrives, instead of created.
  // make sure it has the chapter.relay field set.
  private def ensureChapterRelayField(chapter: Chapter, game: RelayGame, delaySeconds: Int)(using
      RelayTour
  ): Fu[Chapter] =
    val ids = chapterFideIds(game)
    if chapter.relay.exists(_.fideIds == ids) then fuccess(chapter)
    else
      val desiredRelay = makeRelayFor(game, game.root.mainlinePath, delaySeconds)
      for _ <- chapterRepo.setRelay(chapter.id, desiredRelay)
      yield chapter.copy(relay = desiredRelay.some)

  private type NbMoves = Int

  private def forceTailMovesAsVariations(chapter: Chapter, gameMainline: UciPath)(using
      by: Who
  ): Fu[Unit] =
    // tail moves that are not in the source but are in the study chapter,
    // should become forced variations in the study chapter
    gameMainline.nonEmpty.so:
      chapter.root
        .nodeAt(gameMainline)
        .map:
          _.children.toList.filterNot(_.forceVariation).map(gameMainline + _.id)
        .foldMap(_.sequentiallyVoid: childPath =>
          studyApi.forceVariation(
            studyId = chapter.studyId,
            position = Position(chapter, childPath).ref,
            force = true
          )(by))

  private def sendLastNode(
      study: Study,
      chapter: Chapter,
      game: RelayGame,
      gameMainlinePath: UciPath,
      delaySeconds: Int,
      newEnd: Boolean
  )(using Who, RelayTour): Funit =
    // the chapter already has all the game moves,
    // but its relayPath might be out of sync. This can happen if the broadcast
    // has contributors who use REC to record and share variations while the broadcast is ongoing.
    // If they record a variation that is then played out by the broadcast players, then there are
    // no moves to add and send to clients, but the relayPath needs to be updated,
    // both in the database, and in the clients browsers.
    // To achieve this without adding a new websocket event type, we send the last game move again,
    // which contains the relayPath.
    ((newEnd || (game.points.isEmpty && chapter.tags.points.isEmpty)) &&
      chapter.relay.exists(_.path != gameMainlinePath)).so:
      game.root.children
        .nodeAt(gameMainlinePath)
        .so: lastMainlineNode =>
          studyApi.addNode:
            AddNode(
              studyId = study.id,
              positionRef = Position(chapter, gameMainlinePath.parent).ref,
              node = (_, _) => Right(lastMainlineNode),
              opts = moveOpts,
              relay = makeRelayFor(game, gameMainlinePath, delaySeconds).some
            )

  private def addNode(
      study: Study,
      chapter: Chapter,
      game: RelayGame,
      path: UciPath,
      node: Branch,
      delaySeconds: Int
  )(using Who, RelayTour): Funit =
    for
      position = Position(chapter, path).ref
      _ <- node.mainline.foldM(position): (position, n) =>
        val node = AddNode(
          studyId = study.id,
          positionRef = position,
          node = (_, _) => Right(n),
          opts = moveOpts,
          relay = makeRelayFor(game, position.path + n.id, delaySeconds).some
        )
        studyApi.addNode(node).inject(position + n)
    yield ()

  private def setClock(
      chapter: Chapter,
      study: Study,
      game: RelayGame,
      path: UciPath,
      existing: Node,
      current: Node,
      delaySeconds: Int,
      staleSnapshot: Boolean
  )(using by: Who): Fu[Boolean] =
    val mainlinePath = game.root.mainlinePath
    if (staleSnapshot && (path == mainlinePath || path == mainlinePath.parent)) ||
      RelayClock.preserveActiveClock(chapter, game, path, existing)
    then fuccess(false)
    else
      current.clock.fold(fuccess(false)): clock =>
        val clockChanged = existing.clock.forall: prev =>
          ~clock.trust && clock.centis != prev.centis
        val clockAt = RelayClock.updatedClockAt(chapter, game, path, clockChanged, delaySeconds, nowInstant)
        if !clockChanged && clockAt.isEmpty then fuccess(false)
        else
          studyApi
            .setClock(
              studyId = study.id,
              position = Position(chapter, path).ref,
              clock = clock,
              relayClockAt = clockAt
            )(by)
            .inject(true)

  private def getNewNodeOrSetClockOfExisting(
      chapter: Chapter,
      study: Study,
      game: RelayGame,
      delaySeconds: Int,
      staleSnapshot: Boolean
  )(using Who): Fu[(UciPath, Option[Branch], Boolean)] =
    for
      rootClockUpdated <- setClock(
        chapter,
        study,
        game,
        UciPath.root,
        chapter.root,
        game.root,
        delaySeconds,
        staleSnapshot
      )
      result <- game.root.mainline.foldM((UciPath.root, none[Branch], rootClockUpdated)):
        case ((parentPath, None, updated), gameNode) =>
          val path = parentPath + gameNode.id
          chapter.root.nodeAt(path) match
            case None => fuccess((parentPath, gameNode.some, updated))
            case Some(existing) =>
              setClock(chapter, study, game, path, existing, gameNode, delaySeconds, staleSnapshot)
                .map(clockUpdated => (path, none, updated || clockUpdated))
        case (found, _) => fuccess(found)
    yield result

  private def updateChapterTree(
      study: Study,
      chapter: Chapter,
      game: RelayGame,
      delaySeconds: Int,
      staleSnapshot: Boolean,
      newEnd: Boolean
  )(using RelayTour): Fu[(NbMoves, Boolean)] =
    given Who = who(chapter.ownerId)
    for
      gameMainlinePath = game.root.mainlinePath
      (path, newNodeOpt, clockUpdated) <-
        getNewNodeOrSetClockOfExisting(chapter, study, game, delaySeconds, staleSnapshot)
      _ <- forceTailMovesAsVariations(chapter, gameMainlinePath)
      _ <- newNodeOpt.fold(sendLastNode(study, chapter, game, gameMainlinePath, delaySeconds, newEnd)):
        newNode => addNode(study, chapter, game, path, newNode, delaySeconds)
    yield (newNodeOpt.so(_.mainline.size), clockUpdated)

  private def updateChapterTags(
      study: Study,
      chapter: Chapter,
      game: RelayGame
  ): Fu[(Option[Tags], Boolean)] = // (newTags, newEnd)
    val gameTags = game.tags.value.foldLeft(Tags(Nil)): (newTags, tag) =>
      if !chapter.tags.value.has(tag) then newTags + tag
      else newTags
    val newEndTag = (
      game.points.isDefined &&
        gameTags(_.Result).isEmpty &&
        !chapter.tags(_.Result).has(game.showResult)
    ).option(Tag(_.Result, game.showResult))
    val tags = newEndTag.fold(gameTags)(gameTags + _)
    val chapterNewTags = tags.value
      .filterNot: tag =>
        tagManualOverride.exists(chapter.id, tag.name)
      .foldLeft(chapter.tags): (chapterTags, tag) =>
        StudyPgnTags(chapterTags + tag)
      .pipe: tags =>
        def fewMoves = Seq(chapter.root, game.root).forall(_.mainline.sizeIs < 2)
        RelayGame.toggleUnplayedTermination(tags, tags.points.isDefined && fewMoves)
      .pipe: tags =>
        RelayReferenceTime
          .latest(chapter.tags(_.ReferenceTime), game.tags(_.ReferenceTime))
          .fold(tags)(value => tags + Tag(_.ReferenceTime, value))
    if chapterNewTags == chapter.tags then fuccess(none -> false)
    else
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
        newEnd = chapter.tags.points.isEmpty && chapterNewTags.points.isDefined
      yield (chapterNewTags.some, newEnd)

  private def onChapterEnd(tour: RelayTour, study: Study, chapter: Chapter): Funit =
    for
      current <- chapterRepo.byIdAndStudy(chapter.id, study.id)
      _ <- current.so(chapterRepo.endRelay)
    yield
      if tour.official && !study.isMember(UserId("no-analysis")) then
        scheduler.scheduleOnce(5.seconds):
          studyApi.analysisRequest(study.id, chapter.id, study.ownerId, official = true)

  private def makeRelayFor(game: RelayGame, path: UciPath, delaySeconds: Int)(using tour: RelayTour) =
    val now = nowInstant
    Chapter.Relay(
      path = path,
      lastMoveAt = path.nonEmpty.option(now),
      fideIds = chapterFideIds(game),
      clockAt = RelayClock.initialClockAt(game, path, delaySeconds, now)
    )

  // we only set the FIDE IDs in official tours
  // because we don't want random users to assign real OTB players to imaginary tournaments
  private def chapterFideIds(game: RelayGame)(using tour: RelayTour) = tour.official.so(game.fideIdsPair)

  private def chapterName(game: RelayGame, order: Chapter.Order): StudyChapterName =
    Chapter.nameFromPlayerTags(game.tags) | StudyChapterName(s"Board $order")

  private def createChapter(study: Study, game: RelayGame, delaySeconds: Int)(using RelayTour): Fu[Chapter] =
    for
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
        relay = makeRelayFor(game, game.root.mainlinePath, delaySeconds).some
      )
      _ <- studyApi.doAddChapter(study, chapter, sticky = false, who(study.ownerId))
    yield chapter

  private val moveOpts = MoveOpts(
    sticky = false,
    promoteToMainline = true
  )

  private val sri = Sri("")
  private def who(userId: UserId) = Who(userId, sri)

  private def vs(tags: Tags) = s"${tags(_.White) | "?"} - ${tags(_.Black) | "?"}"

  private def showSC(study: Study, chapter: Chapter) =
    s"#${study.id} ${chapter.name}"

private[relay] object RelayClock:

  private def hasActiveTagClock(game: RelayGame): Boolean =
    game.tags.clocks(game.root.lastMainlineNode.ply.turn).isDefined

  def initialClockAt(game: RelayGame, path: UciPath, delaySeconds: Int, now: Instant): Option[Instant] =
    if game.points.isDefined || path.isEmpty || path != game.root.mainlinePath || !hasActiveTagClock(game)
    then None
    else game.referenceClockAt(delaySeconds).orElse(Some(now))

  def updatedClockAt(
      chapter: Chapter,
      game: RelayGame,
      path: UciPath,
      clockChanged: Boolean,
      delaySeconds: Int,
      now: Instant
  ): Option[Option[Instant]] =
    val mainlinePath = game.root.mainlinePath
    if mainlinePath.isEmpty || path != mainlinePath.parent || !chapter.relay.exists(_.path == mainlinePath)
    then None
    else
      val previous = chapter.relay.flatMap(_.clockAt)
      val finished = game.points.isDefined ||
        (chapter.tags.points.isDefined && !game.tags(_.Result).has("*"))
      val next =
        if finished then None
        else if hasActiveTagClock(game) then
          game.referenceClockAt(delaySeconds).orElse(if clockChanged then Some(now) else previous)
        else if clockChanged then None
        else previous
      Option.when(next != previous)(next)

  def preserveActiveClock(chapter: Chapter, game: RelayGame, path: UciPath, existing: Node): Boolean =
    val mainlinePath = game.root.mainlinePath
    mainlinePath.nonEmpty && path == mainlinePath.parent && existing.clock.isDefined &&
    !hasActiveTagClock(game) &&
    (chapter.relay.exists(_.clockAt.isDefined) || chapter.tags.points.isDefined || game.points.isDefined)

  def isOlderSnapshot(chapter: Chapter, game: RelayGame): Boolean =
    game.points.isEmpty && chapter.relay.exists(_.path == game.root.mainlinePath) &&
      (for
        previous <- chapter.tags(_.ReferenceTime).flatMap(RelayReferenceTime.parse)
        current <- game.tags(_.ReferenceTime).flatMap(RelayReferenceTime.parse)
      yield current.isBefore(previous)).getOrElse(false)

sealed trait SyncResult:
  val reportKey: String
object SyncResult:
  case class Ok(chapters: List[ChapterResult], plan: RelayUpdatePlan.Plan, in: RelayRound.WithTourAndStudy)
      extends SyncResult:
    def nbMoves = chapters.foldLeft(0)(_ + _.newMoves)
    def hasUpdates = chapters.exists(_.changed)
    val reportKey = "ok"
  case object Timeout extends Exception with SyncResult with util.control.NoStackTrace:
    val reportKey = "timeout"
    override def getMessage = "In progress..."
  case class Error(msg: String) extends SyncResult:
    val reportKey = "error"

  case class ChapterResult(
      id: StudyChapterId,
      tagUpdate: Boolean,
      newMoves: Int,
      newEnd: Boolean,
      clockUpdate: Boolean = false
  ):
    def changed = tagUpdate || newMoves > 0 || clockUpdate

  def roundBusChannel(roundId: RelayRoundId) = s"relaySyncResult:$roundId"
  def tourBusChannel(tourId: RelayTourId) = s"relaySyncResult:$tourId"
  def groupBusChannel(groupId: RelayGroupId) = s"relaySyncResult:$groupId"
