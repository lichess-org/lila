package lila.activity

import lila.analyse.Analysis
import lila.db.dsl._
import lila.game.Game
import lila.user.User
import lila.user.UserRepo.lichessId

final class ActivityReadApi(coll: Coll) {

  import Activity._
  import BSONHandlers._
  import activities._

  def recent(userId: User.ID, days: Int): Fu[List[ActivityView.AsTo]] =
    coll.find($inIds(makeIds(userId, days))).list[Activity]() map { as =>
      as.map { a =>
        ActivityView.AsTo(
          a.pp.date,
          ActivityView(a.games, a.puzzles)
        )
      }
    }

  private def makeIds(userId: User.ID, days: Int): List[Id] =
    Day.recent(days).map { Id(userId, _) }
}
