package lidraughts.draughtsnet

import org.joda.time.DateTime

import draughts.format.{ FEN, Forsyth }

import lidraughts.analyse.{ Analysis, AnalysisRepo }
import lidraughts.game.{ Game, GameRepo, UciMemo }

final class Analyser(
    repo: DraughtsnetRepo,
    uciMemo: UciMemo,
    analysisBuilder: AnalysisBuilder,
    sequencer: lidraughts.hub.FutureSequencer,
    evalCache: DraughtsnetEvalCache,
    limiter: Limiter,
    evalCacheMinNodes: Int
) {

  val maxPlies = 200

  def apply(game: Game, sender: Work.Sender): Fu[Boolean] =
    (game.metadata.analysed ?? AnalysisRepo.exists(game.id)) flatMap {
      case true => fuFalse
      case _ =>
        limiter(sender, ignoreConcurrentCheck = false) flatMap { accepted =>
          accepted ?? {
            makeWork(game, sender) flatMap { work =>
              sequencer {
                repo getSimilarAnalysis work flatMap {
                  // already in progress, do nothing
                  case Some(similar) if similar.isAcquired => funit
                  // queued by system, reschedule for the human sender
                  case Some(similar) if similar.sender.system && !sender.system =>
                    repo.updateAnalysis(similar.copy(sender = sender))
                  // queued for someone else, do nothing
                  case Some(similar) => funit
                  // first request, store
                  case _ =>
                    lidraughts.mon.draughtsnet.analysis.requestCount()
                    evalCache.skipPositions(work.game, evalCacheMinNodes) flatMap { skipPositions =>
                      lidraughts.mon.draughtsnet.analysis.evalCacheHits(skipPositions.size)
                      repo addAnalysis work.copy(skipPositions = skipPositions)
                    }
                }
              }
            }
          } inject accepted
        }
    }

  def apply(gameId: String, sender: Work.Sender): Fu[Boolean] =
    GameRepo game gameId flatMap { _ ?? { apply(_, sender) } }

  def study(req: lidraughts.hub.actorApi.draughtsnet.StudyChapterRequest): Fu[Boolean] =
    AnalysisRepo exists req.chapterId flatMap {
      case true => fuFalse
      case _ => {
        import req._
        val sender = Work.Sender(req.userId.some, none, false, system = lidraughts.user.User isOfficial req.userId)
        limiter(sender, ignoreConcurrentCheck = true) flatMap { accepted =>
          if (!accepted) logger.info(s"Study request declined: ${req.studyId}/${req.chapterId} by $sender")
          accepted ?? {
            val work = makeWork(
              game = Work.Game(
                id = chapterId,
                initialFen = initialFen,
                studyId = studyId.some,
                simulId = none,
                variant = variant,
                moves = moves take maxPlies map (_.uci),
                finalSquare = true
              ),
              startPly = initialFen.map(_.value).flatMap(Forsyth.getColor).fold(0)(_.fold(0, 1)),
              sender = sender
            )
            sequencer {
              repo getSimilarAnalysis work flatMap {
                _.isEmpty ?? {
                  lidraughts.mon.draughtsnet.analysis.requestCount()
                  evalCache.skipPositions(work.game, evalCacheMinNodes) flatMap { skipPositions =>
                    lidraughts.mon.draughtsnet.analysis.evalCacheHits(skipPositions.size)
                    repo addAnalysis work.copy(skipPositions = skipPositions)
                  }
                }
              }
            }
          } inject accepted
        }
      }
    }

  def fromCache(game: Game, allowIncomplete: Boolean = false): Fu[Analysis] =
    AnalysisRepo.byId(game.id) flatMap {
      case Some(analysis) => fuccess(analysis)
      case _ =>
        val sender = Work.Sender(none, none, false, system = true)
        makeWork(game, sender) flatMap { work =>
          analysisBuilder.fromCache(
            work = work,
            evals = work.game.uciList.map(_ => none),
            allowIncomplete = allowIncomplete
          )
        }
    }

  def fromCache(gameId: String): Fu[Analysis] =
    GameRepo game gameId flatMap {
      case Some(game) => fromCache(game)
      case _ => fufail(s"game $gameId not found")
    }

  private def makeWork(game: Game, sender: Work.Sender): Fu[Work.Analysis] = {
    GameRepo.initialFen(game) zip uciMemo.get(game) map {
      case (initialFen, moves) =>
        val moveList = moves.take(maxPlies)
        val dropMoves = game.situation.ghosts
        makeWork(
          game = Work.Game(
            id = game.id,
            initialFen = initialFen,
            studyId = none,
            simulId = game.simulId,
            variant = game.variant,
            moves = moveList.dropRight(if (game.imported && dropMoves > 1) 1 else dropMoves).toList,
            finalSquare = game.imported
          ),
          startPly = game.draughts.startedAtTurn,
          sender = sender
        )
    }
  }

  private def makeWork(game: Work.Game, startPly: Int, sender: Work.Sender): Work.Analysis =
    Work.Analysis(
      _id = Work.makeId,
      sender = sender,
      game = game,
      startPly = startPly,
      tries = 0,
      lastTryByKey = none,
      acquired = none,
      skipPositions = Nil,
      createdAt = DateTime.now
    )
}
