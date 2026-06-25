package lila.gameSearch

import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.autoconfig.{ *, given }
import lila.search.*

@Module
private class GameSearchConfig(
    @ConfigName("paginator.max_per_page") val paginatorMaxPerPage: MaxPerPage
)

@Module
final class Env(
    appConfig: Configuration,
    gameRepo: lila.core.game.GameRepo,
    userApi: lila.core.user.UserApi,
    elastic: SearchClient
)(using Executor, lila.core.i18n.Translator):

  private val config = appConfig.get[GameSearchConfig]("gameSearch")(using AutoConfig.loader)

  lazy val api = wire[GameSearchApi]

  lazy val paginator = PaginatorBuilder[Game, Query](api, config.paginatorMaxPerPage)

  lazy val forms = wire[GameSearchForm]

  lazy val userGameSearch = wire[UserGameSearch]

  lila.common.Bus.sub[lila.core.game.FinishGame]: finish =>
    val game = finish.game
    (game.finished && !game.sourceIs(_.Import)).so:
      elastic.upsert(SearchClient.Index.Game, game.id)

  lila.common.Bus.sub[lila.core.game.GameAnalysed]: analysed =>
    val game = analysed.game
    (game.userIds.nonEmpty && !game.sourceIs(_.Import)).so:
      elastic.upsert(SearchClient.Index.Game, game.id)

  lila.common.Bus.sub[lila.core.game.DeleteGame]: delete =>
    elastic.delete(SearchClient.Index.Game, delete.id)
