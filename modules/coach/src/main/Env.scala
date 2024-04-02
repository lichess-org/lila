package lila.coach

import akka.actor.ActorSystem
import com.softwaremill.macwire.*
import play.api.Configuration

import lila.core.config.*

@Module
final class Env(
    appConfig: Configuration,
    userRepo: lila.user.UserRepo,
    perfsRepo: lila.user.UserPerfsRepo,
    notifyApi: lila.notify.NotifyApi,
    cacheApi: lila.memo.CacheApi,
    db: lila.db.Db,
    picfitApi: lila.memo.PicfitApi
)(using Executor, ActorSystem):

  private lazy val coachColl = db(CollName("coach"))

  lazy val api = wire[CoachApi]

  lazy val pager = wire[CoachPager]

  lila.common.Bus.subscribeFun("finishGame"):
    case lila.game.actorApi.FinishGame(game, users) if game.rated =>
      if lila.rating.PerfType.standard.has(game.perfType) then
        users.white.so(api.setRating)
        users.black.so(api.setRating)
