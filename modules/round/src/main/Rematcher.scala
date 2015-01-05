package lila.round

import akka.actor.ActorSelection
import akka.pattern.ask
import chess.format.Forsyth
import chess.Variant._
import chess.{ Game => ChessGame, Board, Clock, Variant, Color => ChessColor, Castles }
import ChessColor.{ White, Black }

import lila.db.api._
import lila.game.tube.gameTube
import lila.game.{ GameRepo, Game, Event, Progress, Pov, Source, AnonCookie, PerfPicker }
import lila.memo.ExpireSetMemo
import lila.user.UserRepo
import makeTimeout.short

private[round] final class Rematcher(
    messenger: Messenger,
    onStart: String => Unit,
    rematch960Cache: ExpireSetMemo) {

  def yes(pov: Pov): Fu[Events] = pov match {
    case Pov(game, color) if (game playerCanRematch color) =>
      (game.opponent(color).isOfferingRematch || game.opponent(color).isAi).fold(
        game.next.fold(rematchJoin(pov))(rematchExists(pov)),
        rematchCreate(pov)
      )
    case _ => ClientErrorException.future("[rematcher] invalid yes " + pov)
  }

  def no(pov: Pov): Fu[Events] = pov match {
    case Pov(game, color) if pov.player.isOfferingRematch => GameRepo save {
      messenger.system(game, _.rematchOfferCanceled)
      Progress(game) map { g => g.updatePlayer(color, _.removeRematchOffer) }
    } inject List(Event.ReloadOwner)
    case Pov(game, color) if pov.opponent.isOfferingRematch => GameRepo save {
      messenger.system(game, _.rematchOfferDeclined)
      Progress(game) map { g => g.updatePlayer(!color, _.removeRematchOffer) }
    } inject List(Event.ReloadOwner)
    case _ => ClientErrorException.future("[rematcher] invalid no " + pov)
  }

  private def rematchExists(pov: Pov)(nextId: String): Fu[Events] =
    GameRepo game nextId flatMap {
      _.fold(rematchJoin(pov))(g => fuccess(redirectEvents(g)))
    }

  private def rematchJoin(pov: Pov): Fu[Events] = for {
    nextGame ← returnGame(pov) map (_.start)
    nextId = nextGame.id
    _ ← (GameRepo insertDenormalized nextGame) >>
      GameRepo.saveNext(pov.game, nextGame.id) >>-
      messenger.system(pov.game, _.rematchOfferAccepted) >>- {
        if (pov.game.variant == Chess960 && !rematch960Cache.get(pov.game.id))
          rematch960Cache.put(nextId)
      }
  } yield {
    onStart(nextGame.id)
    redirectEvents(nextGame)
  }

  private def rematchCreate(pov: Pov): Fu[Events] = GameRepo save {
    messenger.system(pov.game, _.rematchOfferSent)
    Progress(pov.game) map { g => g.updatePlayer(pov.color, _ offerRematch) }
  } inject List(Event.ReloadOwner)

  private def returnGame(pov: Pov): Fu[Game] = for {
    initialFen <- GameRepo initialFen pov.game.id
    situation = initialFen flatMap Forsyth.<<<
    pieces = pov.game.variant match {
      case Chess960 =>
        if (rematch960Cache.get(pov.game.id)) Chess960.pieces
        else situation.fold(Chess960.pieces)(_.situation.board.pieces)
      case FromPosition => situation.fold(Standard.pieces)(_.situation.board.pieces)
      case variant      => variant.pieces
    }
    whitePlayer ← returnPlayer(pov.game, White)
    blackPlayer ← returnPlayer(pov.game, Black)
  } yield Game.make(
    game = ChessGame(
      board = Board(pieces, variant = pov.game.variant),
      clock = pov.game.clock map (_.reset),
      turns = situation ?? (_.turns),
      startedAtTurn = situation ?? (_.turns)),
    whitePlayer = whitePlayer,
    blackPlayer = blackPlayer,
    mode = pov.game.mode,
    variant = pov.game.variant,
    source = pov.game.source | Source.Lobby,
    daysPerTurn = pov.game.daysPerTurn,
    castles = situation.fold(Castles.init)(_.situation.board.history.castles),
    pgnImport = None)

  private def returnPlayer(game: Game, color: ChessColor): Fu[lila.game.Player] = {
    val player = lila.game.Player.make(color = color, aiLevel = game.opponent(color).aiLevel)
    game.player(!color).userId.fold(fuccess(player)) { userId =>
      UserRepo byId userId map {
        _.fold(player) { u =>
          player.withUser(u.id, PerfPicker.mainOrDefault(game)(u.perfs))
        }
      }
    }
  }

  private def redirectEvents(game: Game): Events = {
    val whiteId = game fullIdOf White
    val blackId = game fullIdOf Black
    List(
      Event.RedirectOwner(White, blackId, AnonCookie.json(game, Black)),
      Event.RedirectOwner(Black, whiteId, AnonCookie.json(game, White)),
      // tell spectators to reload
      Event.Reload)
  }
}
