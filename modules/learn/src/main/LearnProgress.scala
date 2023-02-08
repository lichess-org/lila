package lila.learn

import org.joda.time.DateTime

case class LearnProgress(
    _id: UserId,
    stages: Map[String, StageProgress],
    createdAt: DateTime,
    updatedAt: DateTime
):

  inline def id = _id

  def withScore(stage: String, level: Int, s: StageProgress.Score) =
    copy(
      stages = stages + (
        stage -> stages.getOrElse(stage, StageProgress.empty).withScore(level, s)
      ),
      updatedAt = DateTime.now
    )

object LearnProgress:

  def empty(id: UserId) =
    LearnProgress(
      _id = id,
      stages = Map.empty,
      createdAt = DateTime.now,
      updatedAt = DateTime.now
    )
