package lila.gameSearch

import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.autoconfig.{ *, given }
import lila.core.config.ConfigName
import lila.core.game.{ FinishGame, InsertGame }
import lila.search.*
import lila.search.client.SearchClient
import lila.search.spec.Query

@Module
private class GameSearchConfig(
    @ConfigName("index") val indexName: String,
    @ConfigName("paginator.max_per_page") val paginatorMaxPerPage: MaxPerPage
)

@Module
final class Env(
    appConfig: Configuration,
    gameRepo: lila.core.game.GameRepo,
    userApi: lila.core.user.UserApi,
    client: SearchClient
)(using Executor, Scheduler, lila.core.i18n.Translator):

  private val config = appConfig.get[GameSearchConfig]("gameSearch")(AutoConfig.loader)

  lazy val api = wire[GameSearchApi]

  lazy val paginator = PaginatorBuilder[Game, Query.Game](api, config.paginatorMaxPerPage)

  lazy val forms = wire[GameSearchForm]

  lazy val userGameSearch = wire[UserGameSearch]

  lila.common.Bus.subscribeFun("finishGame", "gameSearchInsert"):
    case FinishGame(game, _) if !game.aborted => api.store(game)
    case InsertGame(game)                     => api.store(game)
