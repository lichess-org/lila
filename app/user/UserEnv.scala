package lila.app
package user

import com.mongodb.casbah.MongoCollection

import chess.EloCalculator
import elo.EloUpdater
import core.Settings

final class UserEnv(
    settings: Settings,
    mongodb: String ⇒ MongoCollection) {

  import settings._

  lazy val historyRepo = new HistoryRepo(mongodb(UserCollectionHistory))

  lazy val userRepo = new UserRepo(
    collection = mongodb(UserCollectionUser))

  lazy val paginator = new PaginatorBuilder(
    userRepo = userRepo,
    countUsers = () ⇒ cached.countEnabled,
    maxPerPage = UserPaginatorMaxPerPage)

  lazy val eloUpdater = new EloUpdater(
    userRepo = userRepo,
    historyRepo = historyRepo,
    floor = UserEloUpdaterFloor)

  lazy val usernameMemo = new UsernameMemo(timeout = MemoUsernameTimeout)

  lazy val cached = new Cached(
    userRepo = userRepo,
    nbTtl = UserCachedNbTtl)

  lazy val eloChart = EloChart(historyRepo) _
}
