package lila.practice

import scala.concurrent.duration._

import lila.db.dsl._
import lila.memo.AsyncCache
import lila.study.{ Chapter, Study }
import lila.user.User

final class PracticeApi(
    coll: Coll,
    configStore: lila.memo.ConfigStore[PracticeConfig],
    studyApi: lila.study.StudyApi) {

  import BSONHandlers._

  def get(user: Option[User]): Fu[UserPractice] = for {
    struct <- structure.get
    prog <- user.fold(fuccess(PracticeProgress.anon))(progress.get)
  } yield UserPractice(struct, prog)

  def getStudyWithFirstOngoingChapter(user: Option[User], studyId: Study.Id): Fu[Option[UserStudy]] = for {
    up <- get(user)
    chapters <- studyApi.chapterMetadatas(studyId)
    chapterId = up.progress firstOngoingIn chapters.map(_.id)
    studyOption <- chapterId.fold(studyApi byIdWithFirstChapter studyId) {
      studyApi.byIdWithChapter(studyId, _)
    }
  } yield makeUserStudy(studyOption, up, chapters)

  def getStudyWithChapter(user: Option[User], studyId: Study.Id, chapterId: Chapter.Id): Fu[Option[UserStudy]] = for {
    up <- get(user)
    chapters <- studyApi.chapterMetadatas(studyId)
    studyOption <- studyApi.byIdWithChapter(studyId, chapterId)
  } yield makeUserStudy(studyOption, up, chapters)

  private def makeUserStudy(studyOption: Option[Study.WithChapter], up: UserPractice, chapters: List[Chapter.Metadata]) = for {
    rawSc <- studyOption
    sc = rawSc.copy(
      study = rawSc.study.rewindTo(rawSc.chapter).withoutMembers,
      chapter = rawSc.chapter.withoutChildren)
    practiceStudy <- up.structure study sc.study.id
    if up.structure hasStudy sc.study.id
  } yield UserStudy(up, practiceStudy, chapters, sc)

  object config {
    def get = configStore.get map (_ | PracticeConfig.empty)
    def set = configStore.set _
    def form = configStore.makeForm
  }

  object structure {
    private val cache = AsyncCache.single[PracticeStructure](
      "practice.structure",
      f = for {
        conf <- config.get
        chapters <- studyApi.chapterIdNames(conf.studyIds)
      } yield PracticeStructure.make(conf, chapters),
      timeToLive = 3.hours)

    def get = cache(true)
    def clear = cache.remove(true)
  }

  object progress {

    import PracticeProgress.NbMoves

    def get(user: User): Fu[PracticeProgress] =
      coll.uno[PracticeProgress]($id(user.id)) map { _ | PracticeProgress.empty(PracticeProgress.Id(user.id)) }

    private def save(p: PracticeProgress): Funit =
      coll.update($id(p.id), p, upsert = true).void

    def setNbMoves(user: User, chapterId: Chapter.Id, score: NbMoves) =
      get(user) flatMap { prog =>
        save(prog.withNbMoves(chapterId, score))
      }

    def reset(user: User) =
      coll.remove($id(user.id)).void
  }
}
