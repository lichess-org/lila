package lila.mod

import lila.analyse.Analysis
import lila.analyse.{ Analysis, AnalysisRepo }
import lila.db.Types.Coll
import lila.evaluation.GamePool.Analysed
import lila.evaluation.{ GameGroupCrossRef, GameGroupResult, GameGroup }
import lila.game.Game
import lila.game.{ Game, GameRepo }
import reactivemongo.bson._
import scala.concurrent._
import scala.util.{ Success, Failure }

import chess.Color

final class AssessApi(collRef: Coll, collRes: Coll, logApi: ModlogApi) {

  private implicit val gameGroupCrossRefBSONhandler = Macros.handler[GameGroupCrossRef]
  private implicit val gameGroupResultBSONhandler = Macros.handler[GameGroupResult]

  def createReference(assessed: GameGroupCrossRef, mod: String) =
    collRef.update(BSONDocument("_id" -> assessed._id), assessed, upsert = true) >>
      logApi.assessGame(mod, assessed.gameId, assessed.color, assessed.assessment)

  def createResult(result: GameGroupResult) =
    collRes.update(BSONDocument("_id" -> result._id), result, upsert = true)

  def getReferences(max: Int): Fu[List[GameGroupCrossRef]] = collRef.find(BSONDocument())
    .cursor[GameGroupCrossRef]
    .collect[List](max)

  def getReferenceById(id: String) = collRef.find(BSONDocument("_id" -> id))
    .one[GameGroupCrossRef]

  def getResults(username: String, nb: Int = 100) = collRef.find(BSONDocument("username" -> username))
    .cursor[GameGroupResult]
    .collect[List](nb)

  def onAnalysisReady(game: Game, analysis: Analysis) {
    def gameGroupRefs: Fu[List[GameGroup]] =
      getReferences(200) flatMap { references =>
        GameRepo.gameOptions(references.map(_.gameId)) flatMap { games =>
          AnalysisRepo.doneByIds(references.map(_.gameId)) map { analyses =>
            references zip games zip analyses flatMap {
              case ((reference, Some(game)), Some(analysisOption)) => Some(GameGroup(
                Analysed(game, Some(analysisOption)),
                Color(reference.color == "white"), Some(reference.assessment)))
              case _ => None
            }
          }
        }
      }

    def bestMatch(source: GameGroup, refs: Fu[List[GameGroup]]) /*: Fu[Option[GameGroupResult]] = */ {
      refs map {
        ref => println(ref)
      }
      /*
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
      */
    }

    println(s"Assess analysed game ${game.id}")

    val whiteGroup = GameGroup(Analysed(game, Some(analysis)), Color.White)
    val blackGroup = GameGroup(Analysed(game, Some(analysis)), Color.Black)

    gameGroupRefs
  }
}
