package lila.learn

import lila.db.dsl._
import lila.user.User

final class LearnApi(coll: Coll) {

  import BSONHandlers._

  def get(user: User): Fu[LearnProgress] =
    coll.uno[LearnProgress]($id(user.id)) map { _ | LearnProgress.empty(LearnProgress.Id(user.id)) }

  private def save(p: LearnProgress): Funit =
    coll.update($id(p.id), p, upsert = true).void

  def setScore(user: User, stage: String, level: Int, score: StageProgress.Score) =
    get(user) flatMap { prog =>
      save(prog.withScore(stage, level, score))
    }

  def reset(user: User) =
    coll.remove($id(user.id)).void
}
