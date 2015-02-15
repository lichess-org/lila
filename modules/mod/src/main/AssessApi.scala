package lila.mod

import lila.analyse.{ Analysis, AnalysisRepo }
import lila.db.Types.Coll
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.evaluation.{ PlayerAssessment, GameGroupResult, GameResults, GameGroup, Analysed, PeerGame, MatchAndSig }
import lila.game.Game
import lila.game.{ Game, GameRepo }
import org.joda.time.DateTime
import reactivemongo.bson._
import scala.concurrent._

import chess.Color


final class AssessApi(collRef: Coll, collRes: Coll, logApi: ModlogApi) {

  private implicit val bestMatchBSONhandler = Macros.handler[PeerGame]
  private implicit val playerAssessmentBSONhandler = Macros.handler[PlayerAssessment]
  private implicit val gameGroupResultBSONhandler = Macros.handler[GameGroupResult]

  def createPlayerAssessment(assessed: PlayerAssessment) =
    collRef.update(BSONDocument("_id" -> assessed._id), assessed, upsert = true) >>
      logApi.assessGame(assessed.by, assessed.gameId, assessed.color.name, assessed.assessment) >>
        refreshAssess(assessed.gameId)

  def createResult(result: GameGroupResult) =
    collRes.update(BSONDocument("_id" -> result._id), result, upsert = true).void

  def getPlayerAssessments(max: Int): Fu[List[PlayerAssessment]] = collRef.find(BSONDocument())
    .cursor[PlayerAssessment]
    .collect[List](max)

  def getPlayerAssessmentById(id: String) = collRef.find(BSONDocument("_id" -> id))
    .one[PlayerAssessment]

  def getResultsByUserId(userId: String, nb: Int = 100) = collRes.find(BSONDocument("userId" -> userId))
    .sort(BSONDocument("bestMatch.assessment" -> -1, "bestMatch.positiveMatch" -> -1, "bestMatch.matchPercentage" -> -1))
    .cursor[GameGroupResult]
    .collect[List](nb)

  def getResultsByGameIdAndColor(gameId: String, color: Color) = collRes.find(BSONDocument("_id" -> (gameId + "/" + color.name)))
    .one[GameGroupResult]

  def getResultsByGameId(gameId: String): Fu[GameResults] =
    getResultsByGameIdAndColor(gameId, Color.White) zip
        getResultsByGameIdAndColor(gameId, Color.Black) map
            GameResults.tupled

  def refreshAssess(gameId: String): Funit =
    GameRepo.game(gameId) zip
      AnalysisRepo.doneById(gameId) map {
          case (Some(g), Some(a)) => onAnalysisReady(g, a)
          case _ => funit
      }

  def onAnalysisReady(game: Game, analysis: Analysis): Funit = {
    def playerAssessmentGameGroups: Fu[List[GameGroup]] =
      getPlayerAssessments(400) flatMap { assessments =>
        GameRepo.gameOptions(assessments.map(_.gameId)) flatMap { games =>
          AnalysisRepo.doneByIds(assessments.map(_.gameId)) map { analyses =>
            assessments zip games zip analyses flatMap {
              case ((assessment, Some(game)), Some(analysis)) =>
                Some(GameGroup(Analysed(game, analysis), assessment.color, Some(assessment.assessment)))
              case _ => None
            }
          }
        }
      }

    def buildGameGroupResult(source: GameGroup, assessments: List[GameGroup], nb: Int = 5): Option[GameGroupResult] =
      (assessments.map(source.similarityTo) zip assessments).sortBy(-_._1.significance).take(nb).map { 
        case (matchAndSig: MatchAndSig, gameGroup: GameGroup) => 
          PeerGame(
            gameId = gameGroup.analysed.game.id,
            white = gameGroup.color.white,
            positiveMatch = matchAndSig.matches,
            matchPercentage = (100 * matchAndSig.significance).toInt,
            assessment = gameGroup.assessment.getOrElse(1).min(source.analysed.game.wonBy(source.color) match {
              case Some(color) if (color) => 5
              case _ => 3
              })
          )
      } match {
        case a :: b => 
          Some(GameGroupResult(
            _id = source.analysed.game.id + "/" + source.color.name,
            userId = source.analysed.game.player(source.color).userId.getOrElse(""),
            gameId = source.analysed.game.id,
            white = source.color.white,
            bestMatch = a,
            secondaryMatches = b,
            date = DateTime.now,
            sfAvg = source.sfAvg,
            sfSd = source.sfSd,
            mtAvg = source.mtAvg,
            mtSd = source.mtSd,
            blur = source.blurs,
            hold = source.hold
          ))
        case _ => None
      }

    if (!game.isCorrespondence && game.turns >= 40 && game.mode.rated) {
      val whiteGameGroup = GameGroup(Analysed(game, analysis), Color.White)
      val blackGameGroup = GameGroup(Analysed(game, analysis), Color.Black)

      playerAssessmentGameGroups flatMap {
        a => {
          buildGameGroupResult(whiteGameGroup, a).fold(funit){createResult} >>
          buildGameGroupResult(blackGameGroup, a).fold(funit){createResult}
        }
      }
    } else funit
  }
}
