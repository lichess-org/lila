package lila.learn

import lila.db.BSON
import lila.db.dsl._
import reactivemongo.bson._

object BSONHandlers {

  private implicit val StageHandler = new BSONHandler[BSONString, Stage] {
    def read(bs: BSONString) = Stage.byId(Stage.Id(bs.value)) err s"No such Stage: ${bs.value}"
    def write(x: Stage) = BSONString(x.id.value)
  }

  private implicit val StageProgressScoreHandler = intAnyValHandler[StageProgress.Score](_.value, StageProgress.Score.apply)
  private implicit val StageProgressTriesHandler = intAnyValHandler[StageProgress.Tries](_.value, StageProgress.Tries.apply)
  private implicit val StageProgressBSONHandler = Macros.handler[StageProgress]

  private implicit val LearnProgressStagesHandler = BSON.MapDocument.MapHandler[StageProgress]
  implicit val LearnProgressHandler = Macros.handler[LearnProgress]
}
