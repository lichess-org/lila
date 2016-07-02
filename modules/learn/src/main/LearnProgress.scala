package lila.learn

import org.joda.time.DateTime

import lila.user.User

case class LearnProgress(
    _id: LearnProgress.Id,
    stages: Map[String, StageProgress],
    createdAt: DateTime,
    updatedAt: DateTime) {

  def id = _id

  def withScore(stage: String, level: Int, s: StageProgress.Score) = copy(
    stages = stages + (
      stage -> stages.getOrElse(stage, StageProgress empty stage).withScore(level, s)
    ),
    updatedAt = DateTime.now)
}

object LearnProgress {

  sealed trait Id {
    def str: String
  }
  case class UserId(value: String) extends Id {
    def str = value
  }
  case class AnonId(value: String) extends Id {
    def str = s"anon:$value"
  }

  def empty(id: Id) = LearnProgress(
    _id = id,
    stages = Map.empty,
    createdAt = DateTime.now,
    updatedAt = DateTime.now)
}
