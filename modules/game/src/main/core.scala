package lila.game
package core

type ExplorerGame = GameId => Fu[Option[Game]]
type OnTvGame     = Game => Unit

trait GameProxy:
  def updateIfPresent(gameId: GameId)(f: Game => Game): Funit
  def game(gameId: GameId): Fu[Option[Game]]
  def upgradeIfPresent(games: List[Game]): Fu[List[Game]]
  def flushIfPresent(gameId: GameId): Funit

trait RoundJson:
  import play.api.libs.json.JsObject
  def mobileOffline(game: Game, id: GameAnyId): Fu[JsObject]

trait RoundApi:
  def tell(gameId: GameId, msg: Matchable): Unit
  def ask[A](gameId: GameId)(makeMsg: Promise[A] => Matchable): Fu[A]
  def getGames(gameIds: List[GameId]): Fu[List[(GameId, Option[Game])]]
