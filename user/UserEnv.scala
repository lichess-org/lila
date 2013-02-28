package lila.user

import com.mongodb.casbah.MongoCollection
import akka.actor.ActorSystem

import chess.EloCalculator

final class UserEnv(
    settings: Settings,
    mongodb: String ⇒ MongoCollection) {

  import settings._

  lazy val historyRepo = new HistoryRepo(mongodb(CollectionHistory))

  lazy val userRepo = new UserRepo(
    collection = mongodb(CollectionUser))

  lazy val paginator = new PaginatorBuilder(
    userRepo = userRepo,
    countUsers = () ⇒ cached.countEnabled,
    maxPerPage = PaginatorMaxPerPage)

  lazy val eloUpdater = new EloUpdater(
    userRepo = userRepo,
    historyRepo = historyRepo,
    floor = EloUpdaterFloor)

  lazy val usernameMemo = new UsernameMemo(ttl = OnlineTtl)

  lazy val cached = new Cached(
    userRepo = userRepo,
    nbTtl = CachedNbTtl)

  lazy val eloChart = EloChart(historyRepo) _
}
