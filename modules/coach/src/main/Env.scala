package lila.coach

import com.softwaremill.macwire.*
import lila.common.autoconfig.{ *, given }
import play.api.Configuration

import lila.common.config.*
import akka.actor.ActorSystem

@Module
final private class CoachConfig(
    @ConfigName("collection.coach") val coachColl: CollName,
    @ConfigName("collection.review") val reviewColl: CollName
)

@Module
@annotation.nowarn("msg=unused")
final class Env(
    appConfig: Configuration,
    userRepo: lila.user.UserRepo,
    notifyApi: lila.notify.NotifyApi,
    cacheApi: lila.memo.CacheApi,
    db: lila.db.Db,
    picfitApi: lila.memo.PicfitApi
)(using Executor, ActorSystem):

  private val config = appConfig.get[CoachConfig]("coach")(AutoConfig.loader)

  private lazy val coachColl = db(config.coachColl)

  lazy val api = new CoachApi(
    coachColl = coachColl,
    userRepo = userRepo,
    reviewColl = db(config.reviewColl),
    picfitApi = picfitApi,
    notifyApi = notifyApi,
    cacheApi = cacheApi
  )

  lazy val pager = wire[CoachPager]

  lila.common.Bus.subscribeFun(
    "adjustCheater",
    "adjustBooster",
    "finishGame",
    "shadowban",
    "setPermissions"
  ) {
    case lila.hub.actorApi.mod.Shadowban(userId, true) =>
      api.reviews.deleteAllBy(userId).unit
    case lila.hub.actorApi.mod.MarkCheater(userId, true) =>
      api.reviews.deleteAllBy(userId).unit
    case lila.hub.actorApi.mod.MarkBooster(userId) =>
      api.reviews.deleteAllBy(userId).unit
    case lila.game.actorApi.FinishGame(game, white, black) if game.rated =>
      if (game.perfType.exists(lila.rating.PerfType.standard.contains)) {
        white ?? api.setRating
        black ?? api.setRating
      }.unit
  }
