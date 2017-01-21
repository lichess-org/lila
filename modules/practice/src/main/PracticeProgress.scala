package lila.practice

import org.joda.time.DateTime

import lila.study.{ Study, Chapter }

case class PracticeProgress(
    _id: PracticeProgress.Id,
    chapters: PracticeProgress.ChapterNbMoves,
    createdAt: DateTime,
    updatedAt: DateTime) {

  def id = _id

  def apply(chapterId: Chapter.Id): Option[PracticeProgress.NbMoves] =
    chapters get chapterId

  def withNbMoves(chapterId: Chapter.Id, nbMoves: PracticeProgress.NbMoves) = copy(
    chapters = chapters - chapterId + (chapterId -> nbMoves),
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

  case class NbMoves(value: Int) extends AnyVal
  implicit val nbMovesIso = lila.common.Iso.int[NbMoves](NbMoves.apply, _.value)

  type ChapterNbMoves = Map[Chapter.Id, NbMoves]

  def empty(id: Id) = PracticeProgress(
    _id = id,
    chapters = Map.empty,
    createdAt = DateTime.now,
    updatedAt = DateTime.now)
}
