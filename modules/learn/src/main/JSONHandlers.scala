package lila.learn

import play.api.libs.json.*

import lila.common.Json.{ *, given }

object JSONHandlers:

  private given Writes[StageProgress.Score] = writeAs(_.value)
  given OWrites[StageProgress]              = Json.writes

  private given Writes[LearnProgress.Id] = writeAs(_.value)
  given OWrites[LearnProgress]           = Json.writes
