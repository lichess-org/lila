package lila.practice

import reactivemongo.api.bson.Macros.Annotations.Key
import lila.study.ChapterPreview

case class PracticeProgress(
    @Key("_id") id: UserId,
    chapters: PracticeProgress.ChapterNbMoves,
    createdAt: Instant,
    updatedAt: Instant
):

  import PracticeProgress.NbMoves

  def apply(chapterId: StudyChapterId): Option[NbMoves] =
    chapters.get(chapterId)

  def withNbMoves(chapterId: StudyChapterId, nbMoves: PracticeProgress.NbMoves) =
    copy(
      chapters = chapters - chapterId + {
        chapterId -> NbMoves(math.min(chapters.get(chapterId).fold(999)(_.value), nbMoves.value))
      },
      updatedAt = nowInstant
    )

  def countDone(chapterIds: List[StudyChapterId]): Int =
    chapterIds.count(chapters.contains)

  def firstOngoingIn(metas: List[ChapterPreview]): Option[ChapterPreview] =
    metas
      .find: c =>
        !chapters.contains(c.id) && !PracticeStructure.isChapterNameCommented(c.name)
      .orElse:
        metas.find: c =>
          !PracticeStructure.isChapterNameCommented(c.name)

object PracticeProgress:

  opaque type NbMoves = Int
  object NbMoves extends OpaqueInt[NbMoves]

  type ChapterNbMoves = Map[StudyChapterId, NbMoves]

  def empty(id: UserId) = PracticeProgress(
    id = id,
    chapters = Map.empty,
    createdAt = nowInstant,
    updatedAt = nowInstant
  )

  def anon = empty(UserId("anon"))
