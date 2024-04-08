package lila.bookmark

import akka.actor.*
import com.softwaremill.macwire.*
import play.api.Configuration

import lila.core.config.*

@Module
final class Env(
    db: lila.db.Db,
    gameRepo: lila.game.GameRepo,
    gameProxyRepo: lila.game.core.GameProxy
)(using Executor):

  private lazy val bookmarkColl = db(CollName("bookmark"))

  lazy val paginator = wire[PaginatorBuilder]

  lazy val api = wire[BookmarkApi]

  lila.common.Bus.subscribeFun("roundUnplayed"):
    case lila.core.round.DeleteUnplayed(gameId) => api.removeByGameId(gameId)
