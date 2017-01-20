package lila.practice

import org.joda.time.DateTime

case class PracticeProgress(
    _id: PracticeProgress.Id,
    studies: Map[lila.study.Study.Id, StudyProgress],
    createdAt: DateTime,
    updatedAt: DateTime) {

  def id = _id

  // def withNbMoves(chapterId: String, level: Int, s: StageProgress.Score) = copy(
  //   studies = stages + (
  //     stage -> stages.getOrElse(stage, StageProgress empty stage).withScore(level, s)
  //   ),
  //   updatedAt = DateTime.now)
}

object PracticeProgress {

  sealed trait Id extends Any {
    def str: String
  }
  case class UserId(value: String) extends AnyVal with Id {
    def str = value
  }
  case class AnonId(value: String) extends AnyVal with Id {
    def str = s"anon:$value"
  }

  def empty(id: Id) = PracticeProgress(
    _id = id,
    studies = Map.empty,
    createdAt = DateTime.now,
    updatedAt = DateTime.now)
}
