package lila.coach

import com.softwaremill.macwire._

import lila.common.config._
import lila.security.Permission

@Module
final class Env(
    userRepo: lila.user.UserRepo,
    cacheApi: lila.memo.CacheApi,
    db: lila.db.Db,
    imageRepo: lila.db.ImageRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  private lazy val coachColl = db(CollName("coach"))

  private lazy val photographer = new lila.db.Photographer(imageRepo, "coach")

  lazy val api = new CoachApi(
    coachColl = coachColl,
    userRepo = userRepo,
    photographer = photographer,
    cacheApi = cacheApi
  )

  lazy val pager = wire[CoachPager]

  lila.common.Bus.subscribeFun("finishGame", "setPermissions") {
    case lila.hub.actorApi.mod.SetPermissions(userId, permissions) =>
      api.toggleApproved(userId, permissions.has(Permission.Coach.dbKey)).unit
    case lila.game.actorApi.FinishGame(game, sente, gote) if game.rated =>
      if (game.perfType.exists(lila.rating.PerfType.standard.contains)) {
        sente ?? api.setRating
        gote ?? api.setRating
      }.unit
  }

  def cli =
    new lila.common.Cli {
      def process = {
        case "coach" :: "enable" :: username :: Nil  => api.toggleApproved(username, true)
        case "coach" :: "disable" :: username :: Nil => api.toggleApproved(username, false)
      }
    }
}
