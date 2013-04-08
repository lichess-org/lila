package lila.round

import lila.game.{ GameRepo, Game, Pov, Event }
import lila.user.{ User, UserRepo, EloUpdater }
import lila.i18n.I18nKey.{ Select ⇒ SelectI18nKey }
import chess.{ EloCalculator, Status, Color }
import Status._
import Color._
import lila.game.tube.gameTube
import lila.user.tube.userTube
import lila.db.api._
import lila.hub.actorApi.round._

import akka.actor.ActorRef
import play.api.libs.concurrent.Execution.Implicits._
import scalaz.{ OptionTs, Success }

private[round] final class Finisher(
    tournamentOrganizer: ActorRef,
    messenger: Messenger,
    eloUpdater: EloUpdater,
    eloCalculator: EloCalculator,
    finisherLock: FinisherLock,
    indexer: ActorRef) extends OptionTs {

  private type FuEvents = Fu[List[Event]]
  private type ValidFuEvents = Valid[FuEvents]

  def abort(pov: Pov): ValidFuEvents =
    if (pov.game.abortable) finish(pov.game, Aborted)
    else !!("game is not abortable")

  def forceAbort(game: Game): ValidFuEvents =
    if (game.playable) finish(game, Aborted)
    else !!("game is not playable, cannot be force aborted")

  def resign(pov: Pov): ValidFuEvents =
    if (pov.game.resignable) finish(pov.game, Resign, Some(!pov.color))
    else !!("game is not resignable")

  def resignForce(pov: Pov): ValidFuEvents =
    if (pov.game.resignable && !pov.game.hasAi)
      finish(pov.game, Timeout, Some(pov.color))
    else !!("game is not resignable")

  def drawClaim(pov: Pov): ValidFuEvents = pov match {
    case Pov(game, color) if game.playable && game.player.color == color && game.toChessHistory.threefoldRepetition ⇒ finish(game, Draw)
    case Pov(game, color) ⇒ !!("game is not threefold repetition")
  }

  def drawAccept(pov: Pov): ValidFuEvents =
    if (pov.opponent.isOfferingDraw)
      finish(pov.game, Draw, None, Some(_.drawOfferAccepted))
    else !!("opponent is not proposing a draw")

  def drawForce(game: Game): ValidFuEvents = finish(game, Draw, None, None)

  def outoftime(game: Game): ValidFuEvents = game.outoftimePlayer.fold(
    !![FuEvents]("no outoftime applicable " + game.clock.fold("-")(_.remainingTimes.toString))
  ) { player ⇒
      finish(game, Outoftime, Some(!player.color) filter game.toChess.board.hasEnoughMaterialToMate)
    }

  def outoftimes(games: List[Game]): Funit =
    (games map outoftime collect {
      case Success(future) ⇒ future
    }).sequence.void

  def moveFinish(game: Game, color: Color): Fu[List[Event]] =
    (game.status match {
      case Mate                        ⇒ finish(game, Mate, Some(color))
      case status @ (Stalemate | Draw) ⇒ finish(game, status)
      case _                           ⇒ success(fuccess(Nil)): ValidFuEvents
    }) | fuccess(Nil)

  private def finish(
    game: Game,
    status: Status,
    winner: Option[Color] = None,
    message: Option[SelectI18nKey] = None): ValidFuEvents =
    if (finisherLock isLocked game) !!("game finish is locked")
    else success(for {
      _ ← fuccess(finisherLock lock game)
      p1 = game.finish(status, winner)
      p2 ← message.fold(fuccess(p1)) { m ⇒
        messenger.systemMessage(p1.game, m) map p1.++
      }
      _ ← GameRepo save p2
      g = p2.game
      winnerId = winner flatMap (g.player(_).userId)
      _ ← GameRepo.finish(g.id, winnerId)
      _ ← updateElo(g)
      _ ← incNbGames(g, White) doIf (g.status >= Status.Mate)
      _ ← incNbGames(g, Black) doIf (g.status >= Status.Mate)
      _ ← fuccess(indexer ! lila.game.actorApi.InsertGame(g))
      _ ← fuccess(tournamentOrganizer ! FinishGame(g.id))
    } yield p2.events)

  private def incNbGames(game: Game, color: Color): Funit =
    game.player(color).userId zmap { id ⇒
      UserRepo.incNbGames(id, game.rated, game.hasAi,
        result = game.wonBy(color).fold(0)(_.fold(1, -1)).some filterNot (_ ⇒ game.hasAi || game.aborted)
      )
    }

  private def updateElo(game: Game): Funit = ~{
    for {
      whiteUserId ← game.player(White).userId
      blackUserId ← game.player(Black).userId
      if whiteUserId != blackUserId
    } yield (for {
      whiteUser ← optionT($find.byId[User](whiteUserId))
      blackUser ← optionT($find.byId[User](blackUserId))
      _ ← optionT {
        val (whiteElo, blackElo) = eloCalculator.calculate(whiteUser, blackUser, game.winnerColor)
        val (whiteDiff, blackDiff) = (whiteElo - whiteUser.elo, blackElo - blackUser.elo)
        val cheaterWin = (whiteDiff > 0 && whiteUser.engine) || (blackDiff > 0 && blackUser.engine)
        GameRepo.setEloDiffs(game.id, whiteDiff, blackDiff) >>
          eloUpdater.game(whiteUser, whiteElo, blackUser.elo) >>
          eloUpdater.game(blackUser, blackElo, whiteUser.elo) doUnless cheaterWin inject true.some
      }
    } yield ()).value.void
  } doIf (game.finished && game.rated && game.turns >= 2)
}
