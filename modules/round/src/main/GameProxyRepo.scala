package lila.round

import lila.game.{ Game, PlayerRef, Pov }

final class GameProxyRepo(
    gameRepo: lila.game.GameRepo,
    roundSocket: RoundSocket
) {

  def game(gameId: Game.ID): Fu[Option[Game]] = Game.validId(gameId) ?? roundSocket.getGame(gameId)

  def pov(gameId: Game.ID, user: lila.user.User): Fu[Option[Pov]] =
    game(gameId) map { _ flatMap { Pov(_, user) } }

  def pov(gameId: Game.ID, color: chess.Color): Fu[Option[Pov]] =
    game(gameId) map2 { (g: Game) =>
      Pov(g, color)
    }

  def pov(fullId: Game.ID): Fu[Option[Pov]] = pov(PlayerRef(fullId))

  def pov(playerRef: PlayerRef): Fu[Option[Pov]] =
    game(playerRef.gameId) map { _ flatMap { _ playerIdPov playerRef.playerId } }

  def gameIfPresent(gameId: Game.ID): Fu[Option[Game]] = roundSocket gameIfPresent gameId

  def updateIfPresent(game: Game): Fu[Game] =
    if (game.finishedOrAborted) fuccess(game)
    else roundSocket updateIfPresent game

  def updateIfPresent(pov: Pov): Fu[Pov] =
    updateIfPresent(pov.game).dmap(_ pov pov.color)

  def povIfPresent(gameId: Game.ID, color: chess.Color): Fu[Option[Pov]] =
    gameIfPresent(gameId) map2 { (g: Game) =>
      Pov(g, color)
    }

  def povIfPresent(fullId: Game.ID): Fu[Option[Pov]] = povIfPresent(PlayerRef(fullId))

  def povIfPresent(playerRef: PlayerRef): Fu[Option[Pov]] =
    gameIfPresent(playerRef.gameId) map { _ flatMap { _ playerIdPov playerRef.playerId } }

  def urgentGames(user: lila.user.User): Fu[List[Pov]] = gameRepo urgentPovsUnsorted user flatMap {
    _.map { pov =>
      gameIfPresent(pov.gameId) map { _.fold(pov)(pov.withGame) }
    }.sequenceFu map { povs =>
      try {
        povs sortWith Pov.priority
      } catch { case _: IllegalArgumentException => povs sortBy (-_.game.movedAt.getSeconds) }
    }
  }
}
