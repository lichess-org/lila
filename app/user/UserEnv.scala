package lila
package user

import com.mongodb.casbah.MongoCollection

import chess.EloCalculator
import game.GameRepo
import elo.EloUpdater
import core.Settings

final class UserEnv(
    settings: Settings,
    mongodb: String ⇒ MongoCollection,
    gameRepo: GameRepo) {

  import settings._

  lazy val historyRepo = new HistoryRepo(mongodb(MongoCollectionHistory))

  lazy val userRepo = new UserRepo(
    collection = mongodb(MongoCollectionUser))

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

  lazy val userInfo = UserInfo(
    userRepo = userRepo,
    countUsers = () ⇒ cached.countEnabled,
    gameRepo = gameRepo,
    eloCalculator = new EloCalculator,
    eloChartBuilder = eloChart) _
}
