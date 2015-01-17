package lila.mod

import lila.game.Game
import lila.analyse.Analysis
import lila.db.Types.Coll
import lila.game.{ Game, GameRepo }
import lila.analyse.{ Analysis, AnalysisRepo }
import lila.evaluation.{ PlayerAssessment, GameGroupResult, GameGroup, Analysed }
import reactivemongo.bson._
import scala.concurrent._
import scala.util.{Success, Failure}

import chess.Color

final class AssessApi(collRef: Coll, collRes: Coll, logApi: ModlogApi) {

  private implicit val playerAssessmentBSONhandler = Macros.handler[PlayerAssessment]
  private implicit val gameGroupResultBSONhandler = Macros.handler[GameGroupResult]

  def createPlayerAssessment(assessed: PlayerAssessment, mod: String) =
    collRef.update(BSONDocument("_id" -> assessed._id), assessed, upsert = true) >>
      logApi.assessGame(mod, assessed.gameId, assessed.color.name, assessed.assessment)

  def createResult(result: GameGroupResult) =
    collRes.update(BSONDocument("_id" -> result._id), result, upsert = true)

  def getPlayerAssessments: Fu[List[PlayerAssessment]] = collRef.find(BSONDocument())
    .cursor[PlayerAssessment]
    .collect[List]()

  def getPlayerAssessmentById(id: String) = collRef.find(BSONDocument("_id" -> id))
    .one[PlayerAssessment]

  def getResults(username: String, nb: Int = 100) = collRef.find(BSONDocument("username" -> username))
    .cursor[GameGroupResult]
    .collect[List](nb)

  def onAnalysisReady(game: Game, analysis: Analysis) {
    def playerAssessmentGameGroups: Fu[List[GameGroup]] = {
      getPlayerAssessments flatMap {
        _.map { playerAssessment =>
          for {
            optionGameRef <- GameRepo.game(playerAssessment.gameId)
            optionAnalysisRef <- AnalysisRepo.byId(playerAssessment.gameId)
          } yield {
            (optionGameRef, optionAnalysisRef) match {
              case (Some(gameRef), Some(analysisRef)) =>
                Some(GameGroup(Analysed(gameRef, analysisRef), playerAssessment.color, Some(playerAssessment.assessment)))
              case _ => None
            }
          }
        }.sequenceFu.map(_.flatten)
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
