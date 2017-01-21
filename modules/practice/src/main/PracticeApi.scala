package lila.practice

import lila.db.dsl._
import lila.study.Chapter
import lila.user.User

final class PracticeApi(
    coll: Coll,
    configStore: lila.memo.ConfigStore[PracticeStructure]) {

  import BSONHandlers._

  object structure {
    def get = configStore.get map (_ | PracticeStructure.empty)
    def set = configStore.set _
    def form = configStore.makeForm
  }

  object progress {

    def get(user: User): Fu[PracticeProgress] =
      coll.uno[PracticeProgress]($id(user.id)) map { _ | PracticeProgress.empty(PracticeProgress.UserId(user.id)) }

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
