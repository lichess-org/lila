package lila.swiss

import scala.concurrent.duration._

import lila.common.LightUser
import lila.game.Game
import lila.db.dsl._

case class SwissBoard(
    gameId: Game.ID,
    p1: SwissBoard.Player,
    p2: SwissBoard.Player
)

object SwissBoard {
  case class Player(player: SwissPlayer, user: LightUser, rank: Int)
}

final class SwissBoardApi(
    colls: SwissColls,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.user.LightUserApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val cache = cacheApi[Swiss.Id, List[SwissBoard]](64, "swiss.boards") {
    _.expireAfterWrite(15 second).buildAsyncFuture(compute)
  }

  def get(swiss: Swiss) = cache.get(swiss.id)

  private def compute(id: Swiss.Id): Fu[List[SwissBoard]] = ???
}
