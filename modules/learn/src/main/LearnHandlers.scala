package lila.learn

import play.api.libs.json.*
import reactivemongo.api.bson.*

import lila.common.Json.given
import lila.db.dsl.{ *, given }

object LearnHandlers:

  private given BSONHandler[StageProgress] =
    isoHandler[StageProgress, Vector[StageProgress.Score]](_.scores, StageProgress.apply)
  private[learn] given BSONDocumentHandler[LearnProgress] = Macros.handler

  given OWrites[StageProgress] = Json.writes
  given OWrites[LearnProgress] = Json.writes
