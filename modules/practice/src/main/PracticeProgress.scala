package lila.practice

import org.joda.time.DateTime

import lila.user.User
import lila.study.{ Chapter, Study }
import lila.common.Iso

case class PracticeProgress(
    _id: PracticeProgress.Id,
    chapters: PracticeProgress.ChapterNbMoves,
    createdAt: DateTime,
    updatedAt: DateTime
):

  import PracticeProgress.NbMoves

  inline def id = _id

  def apply(chapterId: StudyChapterId): Option[NbMoves] =
    chapters get chapterId

  def withNbMoves(chapterId: StudyChapterId, nbMoves: PracticeProgress.NbMoves) =
    copy(
      chapters = chapters - chapterId + {
        chapterId -> NbMoves(math.min(chapters.get(chapterId).fold(999)(_.value), nbMoves.value))
      },
      updatedAt = DateTime.now
    )

  def countDone(chapterIds: List[StudyChapterId]): Int =
    chapterIds count chapters.contains

  def firstOngoingIn(metas: List[Chapter.Metadata]): Option[Chapter.Metadata] =
    metas.find { c =>
      !chapters.contains(c.id) && !PracticeStructure.isChapterNameCommented(c.name)
    } orElse metas.find { c =>
      !PracticeStructure.isChapterNameCommented(c.name)
    }

object PracticeProgress:

  case class Id(value: String) extends AnyVal

  opaque type NbMoves = Int
  object NbMoves extends OpaqueInt[NbMoves]

  case class OnComplete(userId: User.ID, studyId: StudyId, chapterId: StudyChapterId)

  type ChapterNbMoves = Map[StudyChapterId, NbMoves]

  def empty(id: Id) =
    PracticeProgress(
      _id = id,
      chapters = Map.empty,
      createdAt = DateTime.now,
      updatedAt = DateTime.now
    )

  def anon = empty(Id("anon"))
