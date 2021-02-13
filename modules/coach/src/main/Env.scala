package lila.coach

import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration

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
    imageRepo: lila.db.ImageRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val config = appConfig.get[CoachConfig]("coach")(AutoConfig.loader)

  private lazy val coachColl = db(config.coachColl)

  private lazy val photographer = new lila.db.Photographer(imageRepo, "coach")

  lazy val api = new CoachApi(
    coachColl = coachColl,
    userRepo = userRepo,
    reviewColl = db(config.reviewColl),
    photographer = photographer,
    notifyApi = notifyApi,
    cacheApi = cacheApi
  )

  lazy val pager = wire[CoachPager]

  lila.common.Bus.subscribeFun("adjustCheater", "finishGame", "shadowban", "setPermissions") {
    case lila.hub.actorApi.mod.Shadowban(userId, true) =>
      api.toggleApproved(userId, value = false)
      api.reviews deleteAllBy userId
    case lila.hub.actorApi.mod.MarkCheater(userId, true) =>
      api.toggleApproved(userId, value = false)
      api.reviews deleteAllBy userId
    case lila.hub.actorApi.mod.SetPermissions(userId, permissions) =>
      api.toggleApproved(userId, permissions.has(Permission.Coach.dbKey))
    case lila.game.actorApi.FinishGame(game, white, black) if game.rated =>
      if (game.perfType.exists(lila.rating.PerfType.standard.contains)) {
        white ?? api.setRating
        black ?? api.setRating
      }
    case lila.user.User.GDPRErase(user) => api.reviews deleteAllBy user.id
  }

  def cli =
    new lila.common.Cli {
      def process = {
        case "coach" :: "enable" :: username :: Nil  => api.toggleApproved(username, value = true)
        case "coach" :: "disable" :: username :: Nil => api.toggleApproved(username, value = false)
      }
    }
}
