package lila.learn

import lila.db.dsl._
import lila.user.User

final class LearnApi(coll: Coll) {

  import BSONHandlers._

  def get(user: User): Fu[LearnProgress] =
    coll.uno[LearnProgress]($id(user.id)) map { _ | LearnProgress.empty(user.id) }

  def save(p: LearnProgress): Funit =
    coll.update($id(p.id), p, upsert = true).void
}
