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

  def get(userId: User.ID) = coll.byId[Activity, Id](Id today userId)
}
