package lila
package user

import com.mongodb.casbah.MongoCollection

import chess.EloCalculator
import game.GameRepo
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
    maxPerPage = UserPaginatorMaxPerPage)

  lazy val eloUpdater = new EloUpdater(
    userRepo = userRepo,
    historyRepo = historyRepo,
    floor = UserEloUpdaterFloor)

  lazy val usernameMemo = new UsernameMemo(timeout = MemoUsernameTimeout)

  lazy val cached = new Cached(userRepo)

  lazy val eloChart = EloChart(historyRepo) _

  lazy val userInfo = UserInfo(
    userRepo = userRepo,
    gameRepo = gameRepo,
    eloCalculator = new EloCalculator,
    eloChartBuilder = eloChart) _
}
