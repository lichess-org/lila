package lila.bookmark

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration

import lila.common.config._
import lila.hub.actorApi.bookmark._

@Module
final private class BookmarkConfig(
    @ConfigName("collection.bookmark") val bookmarkCollName: CollName,
    @ConfigName("actor.name") val actorName: String
)

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    gameRepo: lila.game.GameRepo
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  private val config = appConfig.get[BookmarkConfig]("bookmark")(AutoConfig.loader)

  private lazy val bookmarkColl = db(config.bookmarkCollName)

  lazy val paginator = wire[PaginatorBuilder]

  lazy val api = wire[BookmarkApi]

  system.actorOf(
    Props(new Actor {
      def receive = {
        case Toggle(gameId, userId) => api.toggle(gameId, userId).unit
        case Remove(gameId)         => api.removeByGameId(gameId).unit
      }
    }),
    name = config.actorName
  )
}
