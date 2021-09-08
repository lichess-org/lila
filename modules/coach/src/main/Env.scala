package lila.coach

import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import scala.concurrent.duration._

import lila.common.config._
import lila.security.Permission

@Module
final private class CoachConfig(
    @ConfigName("collection.coach") val coachColl: CollName,
    @ConfigName("collection.review") val reviewColl: CollName
)

@Module
final class Env(
    appConfig: Configuration,
    userRepo: lila.user.UserRepo,
    notifyApi: lila.notify.NotifyApi,
    cacheApi: lila.memo.CacheApi,
    db: lila.db.Db,
    picfitApi: lila.memo.PicfitApi,
    imageRepo: lila.db.ImageRepo
)(implicit ec: scala.concurrent.ExecutionContext, system: akka.actor.ActorSystem) {

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

  system.scheduler.scheduleOnce(1 minute) {
    wire[CoachPictureMigration]().unit
  }
}
