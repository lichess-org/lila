package lila.game

import chess.Color
import lila.user.User

case class Pov(game: Game, color: Color):

  def player = game player color

  def playerId = player.id

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

  def millisRemaining: Int =
    game.clock
      .map(_.remainingTime(color).millis.toInt)
      .orElse(game.correspondenceClock.map(_.remainingTime(color).toInt * 1000))
      .getOrElse(Int.MaxValue)

  def hasMoved = game playerHasMoved color

  def moves = game playerMoves color

  def win = game wonBy color

  def loss = game lostBy color

  def forecastable = game.forecastable && game.turnColor != color

  def mightClaimWin = game.forceResignable && !isMyTurn

  def sideAndStart = Game.SideAndStart(color, game.chess.startedAtPly)

  override def toString = ref.toString

object Pov:

  def apply(game: Game): List[Pov] = game.players.mapList(apply(game, _))

  def naturalOrientation(game: Game) = apply(game, game.naturalOrientation)

  def player(game: Game) = apply(game, game.player)

  def apply(game: Game, player: Player) = new Pov(game, player.color)

  def apply(game: Game, playerId: GamePlayerId): Option[Pov] =
    game player playerId map { apply(game, _) }

  def apply[U: UserIdOf](game: Game, user: U): Option[Pov] =
    game player user map { apply(game, _) }

  def ofCurrentTurn(game: Game) = Pov(game, game.turnColor)

  private def orInf(i: Option[Int])     = i getOrElse Int.MaxValue
  private def isFresher(a: Pov, b: Pov) = a.game.movedAt isAfter b.game.movedAt

  def priority(a: Pov, b: Pov) =
    if !a.isMyTurn && !b.isMyTurn then isFresher(a, b)
    else if !a.isMyTurn && b.isMyTurn then false
    else if a.isMyTurn && !b.isMyTurn then true
    // first move has priority over games with more than 30s left
    else if orInf(a.remainingSeconds) < 30 && orInf(b.remainingSeconds) > 30 then true
    else if orInf(b.remainingSeconds) < 30 && orInf(a.remainingSeconds) > 30 then false
    else if !a.hasMoved && b.hasMoved then true
    else if !b.hasMoved && a.hasMoved then false
    else orInf(a.remainingSeconds) < orInf(b.remainingSeconds)

case class PovRef(gameId: GameId, color: Color):

  def unary_! = PovRef(gameId, !color)

  override def toString = s"$gameId/${color.name}"

case class PlayerRef(gameId: GameId, playerId: GamePlayerId)

object PlayerRef:

  def apply(fullId: GameFullId): PlayerRef =
    PlayerRef(fullId.gameId, fullId.playerId)

case class LightPov(game: LightGame, color: Color):
  def gameId   = game.id
  def player   = game player color
  def opponent = game player !color
  // def win      = game wonBy color

object LightPov:

  def apply(game: LightGame, player: LightPlayer): LightPov = LightPov(game, player.color)

  def apply(game: LightGame, userId: UserId): Option[LightPov] =
    game playerByUserId userId map { apply(game, _) }
