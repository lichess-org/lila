package lila.bookmark

import akka.actor.*
import com.softwaremill.macwire.*
import lila.common.autoconfig.{ *, given }
import play.api.Configuration

import lila.common.config.*
import lila.hub.actorApi.bookmark.*

@Module
final private class BookmarkConfig(
    @ConfigName("collection.bookmark") val bookmarkCollName: CollName,
    @ConfigName("actor.name") val actorName: String
)

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    gameRepo: lila.game.GameRepo,
    gameProxyRepo: lila.round.GameProxyRepo
)(using ec: Executor, system: ActorSystem):

  private val config = appConfig.get[BookmarkConfig]("bookmark")(AutoConfig.loader)

  private lazy val bookmarkColl = db(config.bookmarkCollName)

  lazy val paginator = wire[PaginatorBuilder]

  lazy val api = wire[BookmarkApi]

  system.actorOf(
    Props(
      new Actor:
        def receive =
          case Toggle(gameId, userId) => api.toggle(gameId, userId)
          case Remove(gameId)         => api.removeByGameId(gameId)
    ),
    name = config.actorName
  )
