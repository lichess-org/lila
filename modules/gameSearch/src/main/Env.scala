package lila.gameSearch

import com.softwaremill.macwire.*
import lila.common.autoconfig.{ *, given }
import play.api.Configuration

import lila.game.actorApi.{ FinishGame, InsertGame }
import lila.search.*
import lila.common.config.*

@Module
private class GameSearchConfig(
    @ConfigName("index") val indexName: String,
    @ConfigName("paginator.max_per_page") val paginatorMaxPerPage: MaxPerPage,
    @ConfigName("actor.name") val actorName: String
)

@Module
final class Env(
    appConfig: Configuration,
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    makeClient: Index => ESClient
)(using Executor, Scheduler):

  private val config = appConfig.get[GameSearchConfig]("gameSearch")(AutoConfig.loader)

  private lazy val client = makeClient(Index(config.indexName))

  lazy val api = wire[GameSearchApi]

  lazy val paginator = PaginatorBuilder[lila.game.Game, Query](api, config.paginatorMaxPerPage)

  lazy val forms = wire[GameSearchForm]

  lazy val userGameSearch = wire[UserGameSearch]

  lila.common.Bus.subscribeFun("finishGame", "gameSearchInsert"):
    case FinishGame(game, _) if !game.aborted => api.store(game)
    case InsertGame(game)                     => api.store(game)
