package lila.mod

import lila.game.Game
import lila.analyse.Analysis
import lila.db.Types.Coll
import lila.evaluation.{ GameGroupCrossRef, GameGroupResult, GameGroup }
import lila.evaluation.GamePool.Analysed
import reactivemongo.bson._

import chess.Color

final class AssessApi(collRef: Coll, collRes: Coll,logApi: ModlogApi) {

  implicit val gameGroupCrossRefBSONhandler = Macros.handler[GameGroupCrossRef]
  implicit val gameGroupResultBSONhandler = Macros.handler[GameGroupResult]

  def createRef(assessed: GameGroupCrossRef, mod: String) =
    collRef.update(BSONDocument("_id" -> assessed._id), assessed, upsert = true) >>
      logApi.assessGame(mod, assessed.gameId, assessed.color, assessed.assessment)

  def onAnalysisReady(game: Game, analysis: Analysis) {
    println(s"Assess analysed game ${game.id}")
    val whiteGroup = GameGroup(Analysed(game, Some(analysis)), Color.White)
    val blackGroup = GameGroup(Analysed(game, Some(analysis)), Color.Black)


  }
}
