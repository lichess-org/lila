package lila.system

import db._
import model._
import memo.{ VersionMemo, AliveMemo, FinisherLock }
import lila.chess.{ Color, White, Black, EloCalculator }

import scalaz.effects._

final class Finisher(
    historyRepo: HistoryRepo,
    userRepo: UserRepo,
    val gameRepo: GameRepo,
    messenger: Messenger,
    val versionMemo: VersionMemo,
    aliveMemo: AliveMemo,
    eloCalculator: EloCalculator,
    finisherLock: FinisherLock) extends IOTools {

  type ValidIO = Valid[IO[Unit]]

  def abort(pov: Pov): ValidIO =
    if (pov.game.abortable) finish(pov.game, Aborted)
    else !!("game is not abortable")

  def resign(pov: Pov): ValidIO =
    if (pov.game.resignable) finish(pov.game, Resign, Some(!pov.color))
    else !!("game is not resignable")

  def forceResign(pov: Pov): ValidIO =
    if (pov.game.playable && aliveMemo.inactive(pov.game.id, !pov.color))
      finish(pov.game, Timeout, Some(pov.color))
    else !!("game is not force-resignable")

  def drawClaim(pov: Pov): ValidIO = pov match {
    case Pov(game, color) if game.playable && game.player.color == color && game.toChessHistory.threefoldRepetition ⇒ finish(game, Draw)
    case Pov(game, color) ⇒ !!("game is not threefold repetition")
  }

  def drawAccept(pov: Pov): ValidIO =
    if (pov.opponent.isOfferingDraw)
      finish(pov.game, Draw, None, Some("Draw offer accepted"))
    else !!("opponent is not proposing a draw")

  def outoftime(game: DbGame): ValidIO =
    game.outoftimePlayer some { player ⇒
      finish(game, Outoftime,
        Some(!player.color) filter game.toChess.board.hasEnoughMaterialToMate)
    } none !!("no outoftime applicable")

  def outoftimes(games: List[DbGame]): IO[Unit] =
    (games map { g ⇒ outoftime(g) | io() }).sequence map (_ ⇒ Unit)

  def moveFinish(game: DbGame, color: Color): IO[Unit] = (game.status match {
    case Mate                        ⇒ finish(game, Mate, Some(color))
    case status @ (Stalemate | Draw) ⇒ finish(game, status)
    case _                           ⇒ success(io())
  }) | io()

  private def finish(
    game: DbGame,
    status: Status,
    winner: Option[Color] = None,
    message: Option[String] = None): ValidIO =
    if (finisherLock isLocked game) !!("game finish is locked")
    else success(for {
      _ ← finisherLock lock game
      g2 = game.finish(status, winner)
      g3 ← message.fold(messenger.systemMessage(g2, _), io(g2))
      _ ← save(game, g3)
      winnerId = winner flatMap (g3.player(_).userId)
      _ ← gameRepo.finish(g3.id, winnerId)
      _ ← updateElo(g3)
      _ ← incNbGames(g3, White)
      _ ← incNbGames(g3, Black)
    } yield ())

  private def incNbGames(game: DbGame, color: Color): IO[Unit] =
    game.player(color).userId.fold(
      id ⇒ userRepo.incNbGames(id, game.rated),
      io()
    )

  private def updateElo(game: DbGame): IO[Unit] =
    if (!game.finished || !game.rated || game.turns < 2) io()
    else {
      for {
        whiteUserId ← game.player(White).userId
        blackUserId ← game.player(Black).userId
        if whiteUserId != blackUserId
      } yield for {
        whiteUser ← userRepo user whiteUserId
        blackUser ← userRepo user blackUserId
        (whiteElo, blackElo) = eloCalculator.calculate(whiteUser, blackUser, game.winnerColor)
        _ ← gameRepo.setEloDiffs(
          game.id,
          whiteElo - whiteUser.elo,
          blackElo - blackUser.elo)
        _ ← userRepo.setElo(whiteUser.id, whiteElo)
        _ ← userRepo.setElo(blackUser.id, blackElo)
        _ ← historyRepo.addEntry(whiteUser.username, whiteElo, game.id)
        _ ← historyRepo.addEntry(blackUser.username, blackElo, game.id)
      } yield ()
    } | io()

  private def !!(msg: String) = failure(msg.wrapNel)
}
