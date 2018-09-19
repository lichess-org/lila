package lidraughts.draughtsnet

import org.joda.time.DateTime

import draughts.format.FEN
import draughts.format.Uci

import lidraughts.analyse.AnalysisRepo
import lidraughts.game.{ Game, GameRepo, UciMemo }

final class Analyser(
    repo: DraughtsnetRepo,
    uciMemo: UciMemo,
    sequencer: lidraughts.hub.FutureSequencer,
    evalCache: DraughtsnetEvalCache,
    limiter: Limiter
) {

  val maxPlies = 200

  def apply(game: Game, sender: Work.Sender): Fu[Boolean] =
    AnalysisRepo exists game.id flatMap {
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
                    evalCache skipPositions work.game flatMap { skipPositions =>
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
        val sender = Work.Sender(req.userId, none, false, system = req.userId.has("lidraughts"))
        limiter(sender, ignoreConcurrentCheck = true) flatMap { accepted =>
          accepted ?? {
            val work = makeWork(
              game = Work.Game(
                id = chapterId,
                initialFen = initialFen,
                studyId = studyId.some,
                variant = variant,
                moves = moves take maxPlies map (_.uci)
              ),
              startPly = 0,
              sender = sender
            )
            sequencer {
              repo getSimilarAnalysis work flatMap {
                _.isEmpty ?? {
                  lidraughts.mon.draughtsnet.analysis.requestCount()
                  evalCache skipPositions work.game flatMap { skipPositions =>
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

  private def makeWork(game: Game, sender: Work.Sender): Fu[Work.Analysis] = {
    GameRepo.initialFen(game) zip uciMemo.get(game) map {
      case (initialFen, moves) =>
        makeWork(
          game = Work.Game(
            id = game.id,
            initialFen = initialFen map FEN.apply,
            studyId = none,
            variant = game.variant,
            moves = moves take maxPlies toList,
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
