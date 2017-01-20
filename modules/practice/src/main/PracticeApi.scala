package lila.practice

import lila.db.dsl._
import lila.user.User
import lila.study.Chapter

final class PracticeApi(coll: Coll) {

  import BSONHandlers._

  def get(user: User): Fu[PracticeProgress] =
    coll.uno[PracticeProgress]($id(user.id)) map { _ | PracticeProgress.empty(PracticeProgress.UserId(user.id)) }

  private def save(p: PracticeProgress): Funit =
    coll.update($id(p.id), p, upsert = true).void

  def setNbMoves(user: User, fullId: Chapter.FullId, score: StudyProgress.NbMoves) =
    get(user) flatMap { prog =>
      save(prog.withNbMoves(fullId, score))
    }

  def reset(user: User) =
    coll.remove($id(user.id)).void
}
