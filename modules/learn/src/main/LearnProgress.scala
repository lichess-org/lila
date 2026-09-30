package lila.learn

case class LearnProgress(
    _id: UserId,
    stages: Map[String, StageProgress],
    createdAt: Instant,
    updatedAt: Instant
):

  inline def id = _id

  def withScore(stage: String, level: Int, s: StageProgress.Score) =
    copy(
      stages = stages + (
        stage -> stages.getOrElse(stage, StageProgress.empty).withScore(level, s)
      ),
      updatedAt = nowInstant
    )

object LearnProgress:

  def empty(id: UserId) =
    LearnProgress(
      _id = id,
      stages = Map.empty,
      createdAt = nowInstant,
      updatedAt = nowInstant
    )

  import play.api.libs.json.*
  given OWrites[LearnProgress] = progress =>
    Json.obj:
      "stages" -> Json.toJson:
        progress.stages.map((stage, stageProgress) => stage -> stageProgress.scores.map(_.value))
