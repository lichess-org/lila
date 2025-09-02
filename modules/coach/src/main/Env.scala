package lila.coach

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
)(using Executor, Scheduler):

  private lazy val coachColl = db(CollName("coach"))

  lazy val api = wire[CoachApi]

  lazy val pager = wire[CoachPager]

  lila.common.Bus.sub[lila.core.game.FinishGame]:
    case lila.core.game.FinishGame(game, users) if game.rated.yes =>
      if lila.rating.PerfType.standardSet(game.perfKey)
      then users.foreach(u => u.foreach(u => api.updateRatingFromDb(u._1)))
