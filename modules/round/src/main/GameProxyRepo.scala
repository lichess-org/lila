package lila.round

import lila.game.{ Game, PlayerRef, Pov }

final class GameProxyRepo(
    gameRepo: lila.game.GameRepo,
    roundSocket: RoundSocket
)(using Executor):

  def game(gameId: GameId): Fu[Option[Game]] = GameId.validate(gameId) so roundSocket.getGame(gameId)

  def pov[U: UserIdOf](gameId: GameId, user: U): Fu[Option[Pov]] =
    game(gameId) dmap { _ flatMap { Pov(_, user) } }

  def pov(gameId: GameId, color: chess.Color): Fu[Option[Pov]] =
    game(gameId) dmap2 { Pov(_, color) }

  def pov(fullId: GameFullId): Fu[Option[Pov]] = pov(PlayerRef(fullId))

  def pov(playerRef: PlayerRef): Fu[Option[Pov]] =
    game(playerRef.gameId) dmap { _ flatMap { _ playerIdPov playerRef.playerId } }

  def gameIfPresent(gameId: GameId): Fu[Option[Game]] = roundSocket gameIfPresent gameId

  // get the proxied version of the game
  def upgradeIfPresent(game: Game): Fu[Game] =
    if game.finishedOrAborted then fuccess(game)
    else roundSocket upgradeIfPresent game

  def upgradeIfPresent(pov: Pov): Fu[Pov] =
    upgradeIfPresent(pov.game).dmap(_ pov pov.color)

  def upgradeIfPresent(games: List[Game]): Fu[List[Game]] =
    games.map(upgradeIfPresent).parallel

  // update the proxied game
  def updateIfPresent = roundSocket.updateIfPresent

  def povIfPresent(gameId: GameId, color: chess.Color): Fu[Option[Pov]] =
    gameIfPresent(gameId) dmap2 { Pov(_, color) }

  def povIfPresent(fullId: GameFullId): Fu[Option[Pov]] = povIfPresent(PlayerRef(fullId))

  def povIfPresent(playerRef: PlayerRef): Fu[Option[Pov]] =
    gameIfPresent(playerRef.gameId) dmap { _ flatMap { _ playerIdPov playerRef.playerId } }

  def urgentGames[U: UserIdOf](user: U): Fu[List[Pov]] =
    gameRepo urgentPovsUnsorted user flatMap {
      _.map { pov =>
        gameIfPresent(pov.gameId) dmap { _.fold(pov)(pov.withGame) }
      }.parallel map { povs =>
        try povs sortWith Pov.priority
        catch
          case e: IllegalArgumentException =>
            lila.log("round").error(s"Could not sort urgent games of ${user.id}", e)
            povs.sortBy(-_.game.movedAt.toSeconds)
      }
    }
