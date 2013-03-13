package lila.user

import lila.db.ReactiveColl
import com.mongodb.casbah.MongoCollection
import akka.actor.ActorSystem

import chess.EloCalculator

final class UserEnv(settings: Settings, db: String ⇒ ReactiveColl) {

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
