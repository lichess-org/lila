package lila.coach

import lila.security.Permission

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

final class Env(
    config: Config,
    notifyApi: lila.notify.NotifyApi,
    system: ActorSystem,
    db: lila.db.Env
) {

  private val CollectionCoach = config getString "collection.coach"
  private val CollectionReview = config getString "collection.review"
  private val CollectionImage = config getString "collection.image"

  private lazy val coachColl = db(CollectionCoach)
  private lazy val reviewColl = db(CollectionReview)
  private lazy val imageColl = db(CollectionImage)

  private lazy val photographer = new lila.db.Photographer(imageColl, "coach")

  lazy val api = new CoachApi(
    coachColl = coachColl,
    reviewColl = reviewColl,
    photographer = photographer,
    notifyApi = notifyApi
  )

  lazy val pager = new CoachPager(coachColl)

  system.lilaBus.subscribeFun('adjustCheater, 'userActive, 'finishGame, 'shadowban, 'setPermissions) {
    case lila.user.User.Active(user) if !user.seenRecently => api setSeenAt user
    case lila.hub.actorApi.mod.Shadowban(userId, true) =>
      api.toggleApproved(userId, false)
      api.reviews deleteAllBy userId
    case lila.hub.actorApi.mod.MarkCheater(userId, true) =>
      api.toggleApproved(userId, false)
      api.reviews deleteAllBy userId
    case lila.hub.actorApi.mod.SetPermissions(userId, permissions) =>
      api.toggleApproved(userId, permissions.has(Permission.Coach.name))
    case lila.game.actorApi.FinishGame(game, white, black) if game.rated =>
      if (game.perfType.exists(lila.rating.PerfType.standard.contains)) {
        white ?? api.setRating
        black ?? api.setRating
      }
    case lila.user.User.GDPRErase(user) => api.reviews deleteAllBy user.id
  }

  def cli = new lila.common.Cli {
    def process = {
      case "coach" :: "enable" :: username :: Nil => api.toggleApproved(username, true)
      case "coach" :: "disable" :: username :: Nil => api.toggleApproved(username, false)
    }
  }
}

object Env {

  lazy val current: Env = "coach" boot new Env(
    config = lila.common.PlayApp loadConfig "coach",
    notifyApi = lila.notify.Env.current.api,
    system = lila.common.PlayApp.system,
    db = lila.db.Env.current
  )
}
