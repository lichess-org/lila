package lila.system

import db.{ UserRepo, GameRepo, HistoryRepo }
import model._
import memo.{ VersionMemo, AliveMemo, FinisherLock }
import lila.chess.{ Color, White, Black, EloCalculator }

import scalaz.effects._

final class Finisher(
    historyRepo: HistoryRepo,
    userRepo: UserRepo,
    gameRepo: GameRepo,
    versionMemo: VersionMemo,
    aliveMemo: AliveMemo,
    eloCalculator: EloCalculator,
    finisherLock: FinisherLock) {

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

  def claimDraw(pov: Pov): ValidIO = pov match {
    case Pov(game, color) if game.playable && game.player.color == color && game.toChessHistory.threefoldRepetition ⇒ finish(game, Draw, Some(pov.color))
    case Pov(game, color) ⇒ !!("game is not threefold repetition")
  }

  def outoftime(pov: Pov): ValidIO =
    pov.game.outoftimePlayer some { player ⇒
      finish(pov.game, Outoftime,
        Some(!player.color) filter pov.game.toChess.board.hasEnoughMaterialToMate)
    } none !!("no outoftime applicable")

  private def !!(msg: String) = failure(msg.wrapNel)

  private def finish(
    game: DbGame,
    status: Status,
    winner: Option[Color] = None,
    message: Option[String] = None): ValidIO =
    if (finisherLock isLocked game) !!("game finish is locked")
    else success(for {
      _ ← finisherLock lock game
      g2 = game.finish(status, winner, message)
      _ ← gameRepo.applyDiff(game, g2)
      _ ← versionMemo put g2
      _ ← updateElo(g2)
      _ ← incNbGames(g2, White)
      _ ← incNbGames(g2, Black)
    } yield ())

  private def incNbGames(game: DbGame, color: Color) =
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
        _ ← userRepo.setElo(whiteUser.id, whiteElo)
        _ ← userRepo.setElo(blackUser.id, blackElo)
        _ ← historyRepo.addEntry(whiteUser.username, whiteElo, game.id)
        _ ← historyRepo.addEntry(blackUser.username, blackElo, game.id)
      } yield ()
    } | io()
}
