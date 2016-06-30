package lila.learn

import org.joda.time.DateTime

import lila.user.User

case class LearnProgress(
    _id: User.ID,
    stages: Map[String, StageProgress],
    createdAt: DateTime,
    updatedAt: DateTime) {

  def id = _id

  def withScore(stage: String, s: StageProgress.Score) = copy(
    stages = stages + (
      stage -> stages.getOrElse(stage, StageProgress empty stage).withScore(s)
    ),
    updatedAt = DateTime.now)
}

object LearnProgress {

  def empty(userId: User.ID) = LearnProgress(
    _id = userId,
    stages = Map.empty,
    createdAt = DateTime.now,
    updatedAt = DateTime.now)
}
