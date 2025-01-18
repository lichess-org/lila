package lila.study

import alleycats.Zero
import scalalib.actor.AsyncActorSequencers

final private class StudySequencer(
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo
)(using Executor, Scheduler):

  private val workQueue = AsyncActorSequencers[StudyId](
    maxSize = Max(64),
    expiration = 1.minute,
    timeout = 10.seconds,
    name = "study",
    lila.log.asyncActorMonitor.highCardinality
  )

  def sequenceStudy[A <: Matchable: Zero](studyId: StudyId)(f: Study => Fu[A]): Fu[A] =
    workQueue(studyId):
      studyRepo.byId(studyId).flatMapz(f)

  def sequenceStudyWithChapter[A <: Matchable: Zero](studyId: StudyId, chapterId: StudyChapterId)(
      f: Study.WithChapter => Fu[A]
  ): Fu[A] =
    workQueue(studyId):
      studyRepo
        .byIdWithChapter(chapterRepo.coll)(studyId, chapterId)
        .flatMapz(f)
