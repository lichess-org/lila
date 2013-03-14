package lila.user

import lila.db.ReactiveColl

import chess.EloCalculator
import com.typesafe.config.Config

final class UserEnv(config: Config, db: String ⇒ ReactiveColl) {

  val settings = new Settings(config)
  import settings._

  lazy val historyRepo = new HistoryRepo(db(CollectionHistory))

  lazy val userRepo = new UserRepo(db(CollectionUser))

  lazy val paginator = new PaginatorBuilder(
    userRepo = userRepo,
    countUsers = cached.countEnabled,
    maxPerPage = PaginatorMaxPerPage)

  lazy val eloUpdater = new EloUpdater(
    userRepo = userRepo,
    historyRepo = historyRepo,
    floor = EloUpdaterFloor)

  lazy val usernameMemo = new UsernameMemo(ttl = OnlineTtl)

  lazy val cached = new Cached(
    userRepo = userRepo,
    ttl = CachedNbTtl)

  lazy val eloChart = EloChart(historyRepo) _
}
