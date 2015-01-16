package lila.mod

import lila.game.Game
import lila.analyse.Analysis
import lila.db.Types.Coll
import lila.game.{ Game, GameRepo }
import lila.analyse.{ Analysis, AnalysisRepo }
import lila.evaluation.{ GameGroupCrossRef, GameGroupResult, GameGroup }
import lila.evaluation.GamePool.Analysed
import reactivemongo.bson._
import scala.concurrent._
import scala.util.{Success, Failure}

import chess.Color

final class AssessApi(collRef: Coll, collRes: Coll, logApi: ModlogApi) {

  private implicit val gameGroupCrossRefBSONhandler = Macros.handler[GameGroupCrossRef]
  private implicit val gameGroupResultBSONhandler = Macros.handler[GameGroupResult]

  def createReference(assessed: GameGroupCrossRef, mod: String) =
    collRef.update(BSONDocument("_id" -> assessed._id), assessed, upsert = true) >>
      logApi.assessGame(mod, assessed.gameId, assessed.color, assessed.assessment)

  def createResult(result: GameGroupResult) =
    collRes.update(BSONDocument("_id" -> result._id), result, upsert = true)

  def getReferences = collRef.find(BSONDocument())
    .cursor[GameGroupCrossRef]
    .collect[List]()

  def getResults(username: String, nb: Int = 100) = collRef.find(BSONDocument("username" -> username))
    .cursor[GameGroupResult]
    .collect[List](nb)

  def onAnalysisReady(game: Game, analysis: Analysis) {
    println(s"Assess analysed game ${game.id}")

    val whiteGroup = GameGroup(Analysed(game, Some(analysis)), Color.White)
    val blackGroup = GameGroup(Analysed(game, Some(analysis)), Color.Black)

    val referenceGroups = getReferences map {
      _.map {
        crossRef => for {
          optionGameRef <- GameRepo.game(crossRef.gameId)
          optionAnalysisRef <- AnalysisRepo.byId(crossRef.gameId)
        } yield {
          (optionGameRef, optionAnalysisRef) match {
            case (Some(gameRef), Some(analysisRef)) =>
              val gameGroup = GameGroup(Analysed(gameRef, Some(analysisRef)), Color(crossRef.color == "white"), Some(crossRef.assessment))
              val whiteGroupCompared = whiteGroup.similarityTo(gameGroup)
              val blackGroupCompared = blackGroup.similarityTo(gameGroup)
              (
                GameGroupResult( // White
                  _id = game.id + "/white",
                  username = game.whitePlayer.id,
                  sourceGameId = game.id,
                  sourceColor = "white",
                  targetGameId = gameGroup.analysed.game.id,
                  targetColor = gameGroup.color.name,
                  positiveMatch = whiteGroupCompared.matches,
                  matchPercentage = (100 * whiteGroupCompared.significance).toInt
                ),
                GameGroupResult( // Black
                  _id = game.id + "/black",
                  username = game.blackPlayer.id,
                  sourceGameId = game.id,
                  sourceColor = "black",
                  targetGameId = gameGroup.analysed.game.id,
                  targetColor = gameGroup.color.name,
                  positiveMatch = blackGroupCompared.matches,
                  matchPercentage = (100 * blackGroupCompared.significance).toInt
                )
              )
            case _ => fuccess(none)
          }
        }
      }
    }
  }
}
