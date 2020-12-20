package lila.practice

import org.joda.time.DateTime

import lila.user.User
import lila.study.{ Chapter, Study }

case class PracticeProgress(
    _id: PracticeProgress.Id,
    chapters: PracticeProgress.ChapterNbMoves,
    createdAt: DateTime,
    updatedAt: DateTime
) {

  import PracticeProgress.NbMoves

  def id = _id

  def apply(chapterId: Chapter.Id): Option[NbMoves] =
    chapters get chapterId

  def withNbMoves(chapterId: Chapter.Id, nbMoves: PracticeProgress.NbMoves) =
    copy(
      chapters = chapters - chapterId + {
        chapterId -> NbMoves(math.min(chapters.get(chapterId).fold(999)(_.value), nbMoves.value))
      },
      updatedAt = DateTime.now
    )

  def countDone(chapterIds: List[Chapter.Id]): Int =
    chapterIds count chapters.contains

  def firstOngoingIn(metas: List[Chapter.Metadata]): Option[Chapter.Metadata] =
    metas.find { c =>
      !chapters.contains(c.id) && !PracticeStructure.isChapterNameCommented(c.name)
    } orElse metas.find { c =>
      !PracticeStructure.isChapterNameCommented(c.name)
    }
}

object PracticeProgress {

  case class Id(value: String) extends AnyVal

  case class NbMoves(value: Int) extends AnyVal
  implicit val nbMovesIso = lila.common.Iso.int[NbMoves](NbMoves.apply, _.value)

  case class OnComplete(userId: User.ID, studyId: Study.Id, chapterId: Chapter.Id)

  type ChapterNbMoves = Map[Chapter.Id, NbMoves]

  def empty(id: Id) =
    PracticeProgress(
      _id = id,
      chapters = Map.empty,
      createdAt = DateTime.now,
      updatedAt = DateTime.now
    )

  def anon = empty(Id("anon"))
}
