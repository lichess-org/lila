package lila.learn

import org.joda.time.DateTime

import lila.user.User

case class LearnProgress(
    _id: User.ID,
    levels: Map[String, LevelProgress],
    createdAt: DateTime,
    updatedAt: DateTime) {

  def id = _id

  def withScore(level: String, s: LevelProgress.Score) = copy(
    levels = levels + (
      level -> levels.getOrElse(level, LevelProgress empty level).withScore(s)
    ),
    updatedAt = DateTime.now)
}

object LearnProgress {

  def empty(userId: User.ID) = LearnProgress(
    _id = userId,
    levels = Map.empty,
    createdAt = DateTime.now,
    updatedAt = DateTime.now)
}
