package lila.coach

import akka.actor.ActorSystem
import com.softwaremill.macwire.*
import lila.core.config.*

@Module
final class Env(
    perfsRepo: lila.core.user.PerfsRepo,
    userRepo: lila.core.user.UserRepo,
    userApi: lila.core.user.UserApi,
    flagApi: lila.core.user.FlagApi,
    cacheApi: lila.memo.CacheApi,
    db: lila.db.Db,
    picfitApi: lila.memo.PicfitApi
)(using Executor, ActorSystem):

  private lazy val coachColl = db(CollName("coach"))

  lazy val api = wire[CoachApi]

  lazy val pager = wire[CoachPager]

  lila.common.Bus.subscribeFun("finishGame"):
    case lila.core.game.FinishGame(game, users) if game.rated =>
      if lila.rating.PerfType.standardSet(game.perfKey)
      then users.foreach(u => u.foreach(u => api.updateRatingFromDb(u._1)))
