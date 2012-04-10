package lila

import db._
import model._
import memo.{ AliveMemo, FinisherLock }
import chess.{ Color, White, Black, EloCalculator }

import scalaz.effects._

final class Finisher(
    historyRepo: HistoryRepo,
    userRepo: UserRepo,
    gameRepo: GameRepo,
    messenger: Messenger,
    aliveMemo: AliveMemo,
    eloCalculator: EloCalculator,
    finisherLock: FinisherLock) {

  type ValidIOEvents = Valid[IO[List[Event]]]

  def abort(pov: Pov): ValidIOEvents =
    if (pov.game.abortable) finish(pov.game, Aborted)
    else !!("game is not abortable")

  def resign(pov: Pov): ValidIOEvents =
    if (pov.game.resignable) finish(pov.game, Resign, Some(!pov.color))
    else !!("game is not resignable")

  def forceResign(pov: Pov): ValidIOEvents =
    if (pov.game.playable && aliveMemo.inactive(pov.game.id, !pov.color))
      finish(pov.game, Timeout, Some(pov.color))
    else !!("game is not force-resignable")

  def drawClaim(pov: Pov): ValidIOEvents = pov match {
    case Pov(game, color) if game.playable && game.player.color == color && game.toChessHistory.threefoldRepetition ⇒ finish(game, Draw)
    case Pov(game, color) ⇒ !!("game is not threefold repetition")
  }

  def drawAccept(pov: Pov): ValidIOEvents =
    if (pov.opponent.isOfferingDraw)
      finish(pov.game, Draw, None, Some("Draw offer accepted"))
    else !!("opponent is not proposing a draw")

  def outoftime(game: DbGame): ValidIOEvents =
    game.outoftimePlayer some { player ⇒
      finish(game, Outoftime,
        Some(!player.color) filter game.toChess.board.hasEnoughMaterialToMate)
    } none !!("no outoftime applicable " + game.clock)

  def outoftimes(games: List[DbGame]): List[IO[Unit]] =
    games map { g ⇒
      outoftime(g).fold(
        msgs ⇒ putStrLn(g.id + " " + (msgs.list mkString "\n")),
        _ map (_ ⇒ Unit) // events are lost
      ): IO[Unit]
    }

  def moveFinish(game: DbGame, color: Color): IO[List[Event]] =
    (game.status match {
      case Mate                        ⇒ finish(game, Mate, Some(color))
      case status @ (Stalemate | Draw) ⇒ finish(game, status)
      case _                           ⇒ success(io(Nil)): ValidIOEvents
    }) | io(Nil)

  private def finish(
    game: DbGame,
    status: Status,
    winner: Option[Color] = None,
    message: Option[String] = None): Valid[IO[List[Event]]] =
    if (finisherLock isLocked game) !!("game finish is locked")
    else success(for {
      _ ← finisherLock lock game
      p1 = game.finish(status, winner)
      p2 ← message.fold(
        m ⇒ messenger.systemMessage(p1.game, m) map p1.++,
        io(p1)
      )
      _ ← gameRepo save p2
      winnerId = winner flatMap (p2.game.player(_).userId)
      _ ← gameRepo.finish(p2.game.id, winnerId)
      _ ← updateElo(p2.game)
      _ ← incNbGames(p2.game, White)
      _ ← incNbGames(p2.game, Black)
    } yield p2.events)

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
        _ ← historyRepo.addEntry(whiteUser.usernameCanonical, whiteElo, game.id)
        _ ← historyRepo.addEntry(blackUser.usernameCanonical, blackElo, game.id)
      } yield ()
    } | io()

  private def !!(msg: String) = failure(msg.wrapNel)
}
