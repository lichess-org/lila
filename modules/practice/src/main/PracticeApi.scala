package lila.practice

import scala.concurrent.duration._

import lila.db.dsl._
import lila.memo.AsyncCache
import lila.study.Chapter
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
      timeToLive = 1.hour)

    def get = cache(true)
    def clear = cache.clear
  }

  object progress {

    def get(user: User): Fu[PracticeProgress] =
      coll.uno[PracticeProgress]($id(user.id)) map { _ | PracticeProgress.empty(PracticeProgress.Id(user.id)) }

    private def save(p: PracticeProgress): Funit =
      coll.update($id(p.id), p, upsert = true).void

    def setNbMoves(user: User, chapterId: Chapter.Id, score: PracticeProgress.NbMoves) =
      get(user) flatMap { prog =>
        save(prog.withNbMoves(chapterId, score))
      }

    def reset(user: User) =
      coll.remove($id(user.id)).void
  }
}
