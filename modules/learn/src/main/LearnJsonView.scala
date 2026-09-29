package lila.learn

import play.api.libs.json.*

object LearnJsonView:

  /* Mobile only. The best score of each level, per stage, for stages the user has touched.
   * The vector index is the level index, so it can be shorter than the number of levels. */
  def api(progress: LearnProgress) = Json.obj(
    "stages" -> Json.toJson(
      progress.stages.map((stage, stageProgress) => stage -> stageProgress.scores.map(_.value))
    )
  )
