package lila.mod

import lila.db.Types.Coll
import reactivemongo.bson._
import lila.evaluation.{ GameGroupCrossRef }

final class AssessApi(coll: Coll, logApi: ModlogApi) {
  implicit val gameGroupCrossRefBSONhandler = Macros.handler[GameGroupCrossRef]

  def create(assessed: GameGroupCrossRef, mod: String) = 
    coll.update(BSONDocument("_id" -> assessed._id), assessed, upsert = true) >>
      logApi.assessGame(mod, assessed.gameId, assessed.color, assessed.assessment)

}
