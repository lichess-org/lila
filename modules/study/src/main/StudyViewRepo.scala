package lila.study

import lila.db.AsyncColl
import lila.db.dsl.{ *, given }

final private[study] class StudyViewRepo(private val coll: AsyncColl)(using Executor):

  private def makeId(studyId: StudyId, userId: UserId) = s"$studyId/$userId"

  def lastChapter(studyId: StudyId, userId: UserId): Fu[Option[StudyChapterId]] =
    coll(_.primitiveOne[StudyChapterId]($id(makeId(studyId, userId)), "c"))

  def setLastChapter(studyId: StudyId, userId: UserId, chapterId: StudyChapterId): Funit =
    coll:
      _.update.one(
        $id(makeId(studyId, userId)),
        $doc("c" -> chapterId, "d" -> nowInstant),
        upsert = true
      )
    .void
