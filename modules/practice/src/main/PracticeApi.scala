package lila.practice

import lila.db.dsl._
import lila.user.User

final class PracticeApi(coll: Coll) {

  import BSONHandlers._

  // def get(user: User): Fu[PracticeProgress] =
  //   coll.uno[PracticeProgress]($id(user.id)) map { _ | PracticeProgress.empty(PracticeProgress.UserId(user.id)) }

  // private def save(p: PracticeProgress): Funit =
  //   coll.update($id(p.id), p, upsert = true).void

  // def setScore(user: User, stage: String, level: Int, score: StageProgress.Score) =
  //   get(user) flatMap { prog =>
  //     save(prog.withScore(stage, level, score))
  //   }

  // def reset(user: User) =
  //   coll.remove($id(user.id)).void
}
