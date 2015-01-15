package lila.mod

import lila.game.Game
import lila.analyse.Analysis
import lila.db.Types.Coll
import lila.evaluation.{ GameGroupCrossRef }
import reactivemongo.bson._

final class AssessApi(coll: Coll, logApi: ModlogApi) {

  implicit val gameGroupCrossRefBSONhandler = Macros.handler[GameGroupCrossRef]

  def create(assessed: GameGroupCrossRef, mod: String) =
    coll.update(BSONDocument("_id" -> assessed._id), assessed, upsert = true) >>
      logApi.assessGame(mod, assessed.gameId, assessed.color, assessed.assessment)

  def onAnalysisReady(game: Game, analysis: Analysis) {
    println(s"Assess analysed game ${game.id}")
  }
}
