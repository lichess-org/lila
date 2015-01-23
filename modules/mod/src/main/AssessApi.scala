package lila.mod

import lila.analyse.{ Analysis, AnalysisRepo }
import lila.db.Types.Coll
import lila.evaluation.{ PlayerAssessment, GameGroupResult, GameResults, GameGroup, Analysed }
import lila.game.Game
import lila.game.{ Game, GameRepo }
import reactivemongo.bson._
import scala.concurrent._

import chess.Color


final class AssessApi(collRef: Coll, collRes: Coll, logApi: ModlogApi) {

  private implicit val playerAssessmentBSONhandler = Macros.handler[PlayerAssessment]
  private implicit val gameGroupResultBSONhandler = Macros.handler[GameGroupResult]

  def createPlayerAssessment(assessed: PlayerAssessment, mod: String) = {
    collRef.update(BSONDocument("_id" -> assessed._id), assessed, upsert = true) >>
      logApi.assessGame(mod, assessed.gameId, assessed.color.name, assessed.assessment) >>
        refreshAssess(assessed.gameId)
  }


  def createResult(result: GameGroupResult) =
    collRes.update(BSONDocument("_id" -> result._id), result, upsert = true).void

  def getPlayerAssessments(max: Int): Fu[List[PlayerAssessment]] = collRef.find(BSONDocument())
    .cursor[PlayerAssessment]
    .collect[List](max)

  def getPlayerAssessmentById(id: String) = collRef.find(BSONDocument("_id" -> id))
    .one[PlayerAssessment]

  def getResultsByUserId(userId: String, nb: Int = 100) = collRes.find(BSONDocument("userId" -> userId))
    .cursor[GameGroupResult]
    .collect[List](nb)

  def getResultsByGameIdAndColor(gameId: String, color: Color) = collRes.find(BSONDocument("_id" -> (gameId + "/" + color.name)))
    .one[GameGroupResult]

  def getResultsByGameId(gameId: String): Fu[GameResults] =
    getResultsByGameIdAndColor(gameId, Color.White) zip
        getResultsByGameIdAndColor(gameId, Color.Black) map
            GameResults.tupled

  def refreshAssess(gameId: String): Funit = {
    GameRepo.game(gameId) zip
      AnalysisRepo.doneById(gameId) map {
          case (Some(g), Some(a)) => onAnalysisReady(g, a)
          case _ => funit
      }
  }

  def onAnalysisReady(game: Game, analysis: Analysis): Funit = {
    def playerAssessmentGameGroups: Fu[List[GameGroup]] =
      getPlayerAssessments(200) flatMap { assessments =>
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

    def getBestMatch(source: GameGroup, assessments: List[GameGroup]): Option[GameGroupResult] = {
      assessments match {
        case List(best: GameGroup) => {
          val similarityTo = source.similarityTo(best)
          Some(GameGroupResult(
            _id = source.analysed.game.id + "/" + source.color.name,
            userId = source.analysed.game.player(source.color).id,
            sourceGameId = source.analysed.game.id,
            sourceColor = source.color.name,
            targetGameId = best.analysed.game.id,
            targetColor = best.color.name,
            positiveMatch = similarityTo.matches,
            matchPercentage = (100 * similarityTo.significance).toInt,
            assessment = best.assessment.getOrElse(1)
          ))
        }
        case x :: y :: rest => {
          val next = (if (source.similarityTo(x).significance > source.similarityTo(y).significance) x else y) :: rest
          getBestMatch( source, next )
        }
        case Nil => None
      }
    }

    if (!game.isCorrespondence) {
      val whiteGameGroup = GameGroup(Analysed(game, analysis), Color.White)
      val blackGameGroup = GameGroup(Analysed(game, analysis), Color.Black)

      playerAssessmentGameGroups flatMap {
        a => {
          getBestMatch(whiteGameGroup, a).fold(funit){createResult} >>
          getBestMatch(blackGameGroup, a).fold(funit){createResult}
        }
      }
    } else funit
  }
}
