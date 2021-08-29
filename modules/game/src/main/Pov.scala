package lila.game

import chess.Color
import lila.user.User

case class Pov(game: Game, color: Color) {

  def player = game player color

  def playerId = player.id

  def typedPlayerId = Game.PlayerId(player.id)

  def fullId = game fullIdOf color

  def gameId = game.id

  def opponent = game player !color

  def unary_! = Pov(game, !color)

  def flip = Pov(game, !color)

  def ref = PovRef(game.id, color)

  def withGame(g: Game)   = copy(game = g)
  def withColor(c: Color) = copy(color = c)

  lazy val isMyTurn = game.started && game.playable && game.turnColor == color

  lazy val remainingSeconds: Option[Int] =
    game.clock.map(c => c.remainingTime(color).roundSeconds).orElse {
      game.playableCorrespondenceClock.map(_.remainingTime(color).toInt)
    }

  def hasMoved = game playerHasMoved color

  def moves = game playerMoves color

  def win = game wonBy color

  def loss = game lostBy color

  def forecastable = game.forecastable && game.turnColor != color

  def mightClaimWin = game.resignable && !game.hasAi && game.hasClock && !isMyTurn

  override def toString = ref.toString
}

object Pov {

  def apply(game: Game): List[Pov] = game.players.map { apply(game, _) }

  def naturalOrientation(game: Game) = apply(game, game.naturalOrientation)

  def player(game: Game) = apply(game, game.player)

  def apply(game: Game, player: Player) = new Pov(game, player.color)

  def apply(game: Game, playerId: Player.ID): Option[Pov] =
    game player playerId map { apply(game, _) }

  def apply(game: Game, user: User): Option[Pov] =
    game player user map { apply(game, _) }

  def ofUserId(game: Game, userId: User.ID): Option[Pov] =
    game playerByUserId userId map { apply(game, _) }

  def opponentOfUserId(game: Game, userId: String): Option[Player] =
    ofUserId(game, userId) map (_.opponent)

  private def orInf(i: Option[Int]) = i getOrElse Int.MaxValue
  private def isFresher(a: Pov, b: Pov) = {
    val aDate = a.game.movedAt.getSeconds
    val bDate = b.game.movedAt.getSeconds
    if (aDate == bDate) a.gameId < b.gameId
    else aDate > bDate
  }

  def priority(a: Pov, b: Pov) =
    if (!a.isMyTurn && !b.isMyTurn) isFresher(a, b)
    else if (!a.isMyTurn && b.isMyTurn) false
    else if (a.isMyTurn && !b.isMyTurn) true
    // first move has priority over games with more than 30s left
    else if (!a.hasMoved && orInf(b.remainingSeconds) > 30) true
    else if (!b.hasMoved && orInf(a.remainingSeconds) > 30) false
    else if (orInf(a.remainingSeconds) < orInf(b.remainingSeconds)) true
    else if (orInf(b.remainingSeconds) < orInf(a.remainingSeconds)) false
    else isFresher(a, b)
}

case class PovRef(gameId: Game.ID, color: Color) {

  def unary_! = PovRef(gameId, !color)

  override def toString = s"$gameId/${color.name}"
}

case class PlayerRef(gameId: Game.ID, playerId: String)

object PlayerRef {

  def apply(fullId: String): PlayerRef = PlayerRef(Game takeGameId fullId, Game takePlayerId fullId)
}

case class LightPov(game: LightGame, color: Color) {
  def gameId   = game.id
  def player   = game player color
  def opponent = game player !color
  def win      = game wonBy color
}

object LightPov {

  def apply(game: LightGame, player: Player) = new LightPov(game, player.color)

  def ofUserId(game: LightGame, userId: User.ID): Option[LightPov] =
    game playerByUserId userId map { apply(game, _) }
}
