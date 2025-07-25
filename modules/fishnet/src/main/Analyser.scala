package lila.fishnet

import chess.Ply
import scalalib.cache.OnceEvery

import lila.analyse.AnalysisRepo
import lila.core.id
import lila.fishnet.Work.{ Origin, Sender }

final class Analyser(
    repo: FishnetRepo,
    analysisRepo: AnalysisRepo,
    gameRepo: lila.core.game.GameRepo,
    gameApi: lila.core.game.GameApi,
    uciMemo: lila.core.game.UciMemo,
    evalCache: FishnetEvalCache,
    limiter: FishnetLimiter
)(using Executor, Scheduler)
    extends lila.core.fishnet.FishnetRequest:

  private val maxPlies = 300

  private val dedup = OnceEvery[String](2.seconds)

  private val systemSender = Sender(UserId.lichess, none, mod = false, system = true)

  def tutor(gameId: id.GameId) =
    gameRepo
      .game(gameId)
      .orFail(s"No game $gameId")
      .flatMap:
        apply(_, systemSender, Origin.autoTutor.some).void

  def apply(
      game: Game,
      sender: Sender,
      originOpt: Option[Origin] = none
  ): Fu[Analyser.Result] =
    game.metadata.analysed
      .so(analysisRepo.exists(game.id))
      .flatMap:
        if _ then fuccess(Analyser.Result.AlreadyAnalysed)
        else if !gameApi.analysable(game) then fuccess(Analyser.Result.NotAnalysable)
        else
          val origin = originOpt.getOrElse:
            if sender.system then Origin.autoHunter else Origin.manualRequest
          limiter(
            sender,
            ignoreConcurrentCheck =
              sender.system || List(Origin.autoTutor, Origin.autoHunter).contains(origin),
            ownGame = game.userIds contains sender.userId
          ).flatMap { result =>
            (result.ok && dedup(game.id.value))
              .so:
                makeWork(game, sender, origin).flatMap { work =>
                  repo
                    .getSimilarAnalysis(work)
                    .flatMap:
                      // already in progress, do nothing
                      case Some(similar) if similar.isAcquired => funit
                      // queued by system, reschedule for the human sender
                      case Some(similar) if similar.sender.system && !sender.system =>
                        repo.updateAnalysis(similar.copy(sender = sender))
                      // queued for someone else, do nothing
                      case Some(_) => funit
                      // first request, store
                      case _ =>
                        lila.mon.fishnet.analysis.requestCount("game").increment()
                        evalCache
                          .skipPositions(work.game)
                          .monSuccess(_.fishnet.analysis.skipPositionsGame)
                          .flatMap: skipPositions =>
                            lila.mon.fishnet.analysis.evalCacheHits.record(skipPositions.size)
                            repo.addAnalysis(work.copy(skipPositions = skipPositions))
                }
              .inject(result)
          }

  def apply(gameId: GameId, sender: Sender): Fu[Analyser.Result] =
    gameRepo
      .game(gameId)
      .flatMap:
        _.fold[Fu[Analyser.Result]](fuccess(Analyser.Result.NoGame)): game =>
          apply(game, sender)

  def study(req: lila.core.fishnet.Bus.StudyChapterRequest): Fu[Analyser.Result] =
    analysisRepo
      .chapterExists(req.chapterId)
      .flatMap:
        if _ then fuccess(Analyser.Result.NoChapter)
        else
          import req.*
          val sender = Sender(req.userId, none, mod = false, system = false)
          val limitFu =
            if req.official then fuccess(Analyser.Result.Ok)
            else limiter(sender, ignoreConcurrentCheck = true, ownGame = false)
          limitFu.flatMap { result =>
            if !result.ok then
              logger.info(s"Study request declined: ${req.studyId}/${req.chapterId} by $sender")
            (result.ok && dedup(chapterId.value))
              .so:
                val work = makeWork(
                  game = Work.Game(
                    id = chapterId.value,
                    initialFen = initialFen,
                    studyId = studyId.some,
                    variant = variant,
                    moves = moves.take(maxPlies).map(_.uci).mkString(" ")
                  ),
                  // if black moves first, use 1 as startPly so the analysis doesn't get reversed
                  startPly = Ply(initialFen.map(_.colorOrWhite).so(_.fold(0, 1))),
                  sender = sender,
                  origin = if req.official then Origin.officialBroadcast else Origin.manualRequest
                )
                repo
                  .getSimilarAnalysis(work)
                  .flatMap:
                    _.isEmpty.so:
                      lila.mon.fishnet.analysis.requestCount("study").increment()
                      evalCache
                        .skipPositions(work.game)
                        .monSuccess(_.fishnet.analysis.skipPositionsStudy)
                        .withTimeout(2.seconds, s"study analysis skipPositions $work")
                        .recoverDefault
                        .flatMap: skipPositions =>
                          lila.mon.fishnet.analysis.evalCacheHits.record(skipPositions.size)
                          repo.addAnalysis(work.copy(skipPositions = skipPositions))
              .inject(result)
          }

  private def makeWork(game: Game, sender: Sender, origin: Origin): Fu[Work.Analysis] =
    gameRepo.initialFen(game).zip(uciMemo.get(game)).map { (initialFen, moves) =>
      makeWork(
        game = Work.Game(
          id = game.id.value,
          initialFen = initialFen,
          studyId = none,
          variant = game.variant,
          moves = moves.take(maxPlies).mkString(" ")
        ),
        startPly = game.chess.startedAtPly,
        sender = sender,
        origin = origin
      )
    }

  private def makeWork(game: Work.Game, startPly: Ply, sender: Sender, origin: Origin) =
    Work.Analysis(
      _id = Work.makeId,
      sender = sender,
      game = game,
      startPly = startPly,
      tries = 0,
      lastTryByKey = none,
      acquired = none,
      skipPositions = Nil,
      createdAt = nowInstant,
      origin = origin.some
    )

object Analyser:

  enum Result(val error: Option[String]):
    def ok = error.isEmpty
    case Ok extends Result(none)
    case NoGame extends Result("Game not found".some)
    case NoChapter extends Result("Chapter not found".some)
    case AlreadyAnalysed extends Result("This game is already analysed".some)
    case NotAnalysable extends Result("This game is not analysable".some)
    case ConcurrentAnalysis extends Result("You already have an ongoing requested analysis".some)
    case WeeklyLimit extends Result("You have reached the weekly analysis limit".some)
    case DailyLimit extends Result("You have reached the daily analysis limit".some)
    case DailyIpLimit extends Result("You have reached the daily analysis limit on this IP".some)
