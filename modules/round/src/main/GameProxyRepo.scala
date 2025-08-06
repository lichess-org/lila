package lila.round

import lila.core.game.PlayerRef
import lila.game.GameExt.*

final class GameProxyRepo(
    gameRepo: lila.game.GameRepo,
    roundSocket: RoundSocket
)(using Executor):

  export roundSocket.{ updateIfPresent, flushIfPresent }

  def game(gameId: GameId): Fu[Option[Game]] = GameId.validate(gameId).so(roundSocket.getGame(gameId))

  def pov[U: UserIdOf](gameId: GameId, user: U): Fu[Option[Pov]] =
    game(gameId).dmap { _.flatMap { Pov(_, user) } }

  def pov(gameId: GameId, color: Color): Fu[Option[Pov]] =
    game(gameId).dmap2 { Pov(_, color) }

  def pov(fullId: GameFullId): Fu[Option[Pov]] = pov(PlayerRef(fullId))

  def pov(playerRef: PlayerRef): Fu[Option[Pov]] =
    game(playerRef.gameId).dmap { _.flatMap(_.playerIdPov(playerRef.playerId)) }

  def gameIfPresent(gameId: GameId): Fu[Option[Game]] = roundSocket.gameIfPresent(gameId)

  def gameIfPresentOrFetch(gameId: GameId): Fu[Option[Game]] =
    gameIfPresent(gameId).orElse(gameRepo.game(gameId))

  // get the proxied version of the game
  def upgradeIfPresent(game: Game): Fu[Game] =
    if game.finishedOrAborted then fuccess(game)
    else roundSocket.upgradeIfPresent(game)

  def upgradeIfPresent(pov: Pov): Fu[Pov] =
    upgradeIfPresent(pov.game).dmap(_.pov(pov.color))

  def upgradeIfPresent(games: List[Game]): Fu[List[Game]] =
    games.map(upgradeIfPresent).parallel

  def povIfPresent(gameId: GameId, color: Color): Fu[Option[Pov]] =
    gameIfPresent(gameId).dmap2 { Pov(_, color) }

  def povIfPresent(fullId: GameFullId): Fu[Option[Pov]] = povIfPresent(PlayerRef(fullId))

  def povIfPresent(playerRef: PlayerRef): Fu[Option[Pov]] =
    gameIfPresent(playerRef.gameId).dmap { _.flatMap { _.playerIdPov(playerRef.playerId) } }

  def urgentGames[U: UserIdOf](user: U): Fu[List[Pov]] = for
    inDb <- gameRepo.urgentPovsUnsorted(user)
    povs <- inDb.parallel: pov =>
      gameIfPresent(pov.gameId).dmap(_.fold(pov)(pov.withGame))
  yield try povs.sortWith(lila.game.Pov.priority)
  catch
    case e: IllegalArgumentException =>
      lila.log("round").error(s"Could not sort urgent games of ${user.id}", e)
      povs.sortBy(-_.game.movedAt.toSeconds)
