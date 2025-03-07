package lila.learn

import reactivemongo.api.bson.*
import play.api.libs.json.*

import lila.db.dsl.{ *, given }
import lila.common.Json.{ *, given }

object LearnHandlers:

  private given BSONHandler[StageProgress] =
    isoHandler[StageProgress, Vector[StageProgress.Score]](_.scores, StageProgress.apply)
  private[learn] given BSONDocumentHandler[LearnProgress] = Macros.handler

  given OWrites[StageProgress] = Json.writes
  given OWrites[LearnProgress] = Json.writes
