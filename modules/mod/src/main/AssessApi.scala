package lila.mod

import lila.analyse.{ Analysis, AnalysisRepo }
import lila.db.Types.Coll
import lila.evaluation.{ PlayerAssessment, GameGroupResult, GameGroup, Analysed }
import lila.game.Game
import lila.game.{ Game, GameRepo }
import reactivemongo.bson._
import scala.concurrent._

import chess.Color


final class AssessApi(collRef: Coll, collRes: Coll, logApi: ModlogApi) {

  private implicit val playerAssessmentBSONhandler = Macros.handler[PlayerAssessment]
  private implicit val gameGroupResultBSONhandler = Macros.handler[GameGroupResult]

  def createPlayerAssessment(assessed: PlayerAssessment, mod: String) =
    collRef.update(BSONDocument("_id" -> assessed._id), assessed, upsert = true) >>
      logApi.assessGame(mod, assessed.gameId, assessed.color.name, assessed.assessment)

  def createResult(result: GameGroupResult) =
    collRes.update(BSONDocument("_id" -> result._id), result, upsert = true)

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

  def onAnalysisReady(game: Game, analysis: Analysis) {
    def playerAssessmentGameGroups: Fu[List[GameGroup]] =
      getPlayerAssessments(200) flatMap { assessments =>
        GameRepo.gameOptions(assessments.map(_.gameId)) flatMap { games =>
          AnalysisRepo.doneByIds(assessments.map(_.gameId)) map { analyses =>
            assessments zip games zip analyses flatMap {
              case ((assessment, Some(game)), Some(analysisOption)) => 
                Some(GameGroup(Analysed(game, analysisOption), assessment.color, Some(assessment.assessment)))
              case _ => None
            }
          }
        }
      }

    def writeBestMatch(source: GameGroup, assessments: List[GameGroup]) {
      assessments match {
        case List(best: GameGroup) => {
          val similarityTo = source.similarityTo(best)
          createResult(GameGroupResult(
            _id = source.analysed.game.id + "/" + source.color.name,
            userId = source.analysed.game.player(source.color).id,
            sourceGameId = source.analysed.game.id,
            sourceColor = source.color.name,
            targetGameId = best.analysed.game.id,
            targetColor = best.color.name,
            positiveMatch = similarityTo.matches,
            matchPercentage = (100 * similarityTo.significance).toInt
            ))
        }
        case x :: y :: rest => {
          val next = (if (source.similarityTo(x).significance > source.similarityTo(y).significance) x else y) :: rest
          writeBestMatch( source, next )
        }
        case Nil =>
      }
    }

    val whiteGroup = GameGroup(Analysed(game, analysis), Color.White)
    val blackGroup = GameGroup(Analysed(game, analysis), Color.Black)

    playerAssessmentGameGroups map {
      a => {
        writeBestMatch(whiteGroup, a)
        writeBestMatch(blackGroup, a)
      }
    }
  }
}
