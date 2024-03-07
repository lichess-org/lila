package lila.fishnet

import org.joda.time.DateTime
import scala.concurrent.duration._
import scala.util.chaining._

import lila.common.Bus
import lila.analyse.AnalysisRepo
import lila.game.Game

final class Analyser(
    repo: FishnetRepo,
    analysisRepo: AnalysisRepo,
    gameRepo: lila.game.GameRepo,
    evalCache: FishnetEvalCache,
    limiter: Limiter
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) {

  val maxPlies = 225

  private val workQueue = new lila.hub.DuctSequencer(maxSize = 256, timeout = 5 seconds, "fishnetAnalyser")

  def apply(game: Game, sender: Work.Sender): Fu[Boolean] =
    (game.metadata.analysed ?? analysisRepo.exists(game.id)) flatMap {
      case true => fuFalse
      case _ =>
        limiter(sender, ignoreConcurrentCheck = false) flatMap { accepted =>
          accepted ?? {
            makeWork(game, sender) pipe { work =>
              workQueue {
                repo getSimilarAnalysis work flatMap {
                  case Some(similar) => {
                    val maybeUpdatedSimilar = updateAnalysis(similar, sender)
                    maybeUpdatedSimilar.foreach { a =>
                      repo.updateAnalysis(a)
                    }
                    funit
                  }
                  case _ =>
                    lila.mon.fishnet.analysis.requestCount("game").increment()
                    evalCache skipPositions work.game flatMap { skipPositions =>
                      lila.mon.fishnet.analysis.evalCacheHits.record(skipPositions.size)
                      repo addAnalysis work.copy(skipPositions = skipPositions)
                    }
                }
              }
            }
          } inject accepted
        }
    }

  def apply(gameId: String, sender: Work.Sender): Fu[Boolean] =
    gameRepo game gameId flatMap { _ ?? { apply(_, sender) } }

  def postGameStudy(gameId: String, sender: Work.Sender): Fu[Boolean] =
    gameRepo game gameId flatMap {
      _ ?? { g =>
        (g.metadata.analysed ?? analysisRepo.byId(g.id)) flatMap {
          case Some(analysis) =>
            sender.postGameStudy foreach { pgs =>
              Bus.publish(
                lila.analyse.actorApi
                  .StudyAnalysisProgress(analysis.copy(id = pgs.chapterId, studyId = pgs.studyId.some), true),
                "studyAnalysisProgress"
              )
            }
            fuTrue
          case _ => apply(g, sender)
        }
      }
    }

  def study(req: lila.hub.actorApi.fishnet.StudyChapterRequest): Fu[Boolean] =
    analysisRepo exists req.chapterId flatMap {
      case true => fuFalse
      case _ => {
        import req._
        val sender = Work.Sender(req.userId.some, none, none, false, system = false)
        limiter(sender, ignoreConcurrentCheck = true) flatMap { accepted =>
          if (!accepted) logger.info(s"Study request declined: ${req.studyId}/${req.chapterId} by $sender")
          accepted ?? {
            val work = makeWork(
              game = Work.Game(
                id = chapterId,
                initialSfen = initialSfen,
                studyId = studyId.some,
                variant = variant,
                moves = moves take maxPlies map (_.usi) mkString " "
              ),
              puzzleWorthy = false,
              // if gote moves first, use 1 as startPly so the analysis doesn't get reversed
              startPly = initialSfen.flatMap(_.color).??(_.fold(0, 1)),
              sender = sender
            )
            workQueue {
              repo getSimilarAnalysis work flatMap {
                _.isEmpty ?? {
                  lila.mon.fishnet.analysis.requestCount("study").increment()
                  evalCache skipPositions work.game flatMap { skipPositions =>
                    lila.mon.fishnet.analysis.evalCacheHits.record(skipPositions.size)
                    repo addAnalysis work.copy(skipPositions = skipPositions)
                  }
                }
              }
            }
          } inject accepted
        }
      }
    }

  private def updateAnalysis(analysis: Work.Analysis, sender: Work.Sender): Option[Work.Analysis] = {
    val senderUpdate = updateAnalysisSender(analysis, sender)
    val pgsUpdates =
      sender.postGameStudy.flatMap(updateAnalysisPostGameStudies(senderUpdate.getOrElse(analysis), _))

    pgsUpdates.orElse(senderUpdate)
  }

  private def updateAnalysisSender(analysis: Work.Analysis, sender: Work.Sender): Option[Work.Analysis] =
    if (analysis.isAcquired || !analysis.sender.system || sender.system)
      none
    else analysis.copy(sender = sender).some

  private def updateAnalysisPostGameStudies(
      analysis: Work.Analysis,
      postGameStudy: lila.analyse.Analysis.PostGameStudy
  ): Option[Work.Analysis] =
    if (analysis.postGameStudies.contains(postGameStudy)) none
    else analysis.copy(postGameStudies = analysis.postGameStudies + postGameStudy).some

  private def makeWork(game: Game, sender: Work.Sender): Work.Analysis =
    makeWork(
      game = Work.Game(
        id = game.id,
        initialSfen = game.initialSfen,
        studyId = none,
        variant = game.variant,
        moves = game.usis.take(maxPlies).map(_.usi).mkString(" ")
      ),
      startPly = game.shogi.startedAtPly,
      puzzleWorthy = game.userRatings.exists(_ > 1600),
      sender = sender
    )

  private def makeWork(
      game: Work.Game,
      startPly: Int,
      puzzleWorthy: Boolean,
      sender: Work.Sender
  ): Work.Analysis =
    Work.Analysis(
      _id = Work.makeId,
      sender = sender,
      game = game,
      engine = lila.game.EngineConfig.Engine(game.initialSfen, game.variant, none).name,
      startPly = startPly,
      tries = 0,
      lastTryByKey = none,
      acquired = none,
      skipPositions = Nil,
      postGameStudies = sender.postGameStudy.toSet,
      puzzleWorthy = puzzleWorthy,
      createdAt = DateTime.now
    )
}
