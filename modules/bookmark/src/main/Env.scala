package lila.bookmark

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration

import lila.common.config._
import lila.common.tagging._
import lila.hub.actorApi.bookmark._

@Module
private class BookmarkConfig(
    @ConfigName("collection.bookmark") val bookmarkCollName: CollName,
    @ConfigName("paginator.maxPerPage") val paginatorMaxPerPage: MaxPerPage,
    @ConfigName("actor.name") val actorName: String
)

final class Env(
    appConfig: Configuration,
    db: lila.db.Env,
    gameRepo: lila.game.GameRepo
)(implicit system: ActorSystem) {

  private val config = appConfig.get[BookmarkConfig]("game")(AutoConfig.loader)

  private lazy val bookmarkColl = db(config.bookmarkCollName)

  lazy val paginator = wire[PaginatorBuilder]

  lazy val api = wire[BookmarkApi]

  system.actorOf(Props(new Actor {
    def receive = {
      case Toggle(gameId, userId) => api.toggle(gameId, userId)
      case Remove(gameId) => api removeByGameId gameId
    }
  }), name = config.actorName)
}
