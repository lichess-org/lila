package lila.practice

import org.joda.time.DateTime

import lila.study.{ Study, Chapter }

case class PracticeProgress(
    _id: PracticeProgress.Id,
    studies: Map[Study.Id, StudyProgress],
    createdAt: DateTime,
    updatedAt: DateTime) {

  def id = _id

  def apply(fullId: Chapter.FullId): Option[StudyProgress.NbMoves] =
    studies get fullId.studyId flatMap (_ get fullId.chapterId)

  def withNbMoves(fullId: Chapter.FullId, nbMoves: StudyProgress.NbMoves) = copy(
    studies = studies + (
      fullId.studyId -> studies.getOrElse(fullId.studyId, StudyProgress.empty).withNbMoves(fullId.chapterId, nbMoves)
    ),
    updatedAt = DateTime.now)
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
