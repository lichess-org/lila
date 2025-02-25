package lila.bookmark

import com.softwaremill.macwire.*

import lila.core.config.*

@Module
final class Env(
    db: lila.db.Db,
    gameApi: lila.core.game.GameApi,
    gameRepo: lila.core.game.GameRepo
)(using Executor):

  private lazy val bookmarkColl = db(CollName("bookmark"))

  lazy val paginator = wire[PaginatorBuilder]

  lazy val api = wire[BookmarkApi]

  def exists: lila.core.misc.BookmarkExists = api.exists

  lila.common.Bus.subscribeFun("roundUnplayed"):
    case lila.core.round.DeleteUnplayed(gameId) => api.removeByGameId(gameId)
