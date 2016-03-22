package lila.round

import akka.actor.ActorSelection
import akka.pattern.ask
import chess.format.Forsyth
import chess.variant._
import chess.{ Game => ChessGame, Board, Clock, Color => ChessColor, Castles }
import ChessColor.{ White, Black }

import lila.db.api._
import lila.game.{ GameRepo, Game, Event, Progress, Pov, Source, AnonCookie, PerfPicker }
import lila.memo.ExpireSetMemo
import lila.user.{ User, UserRepo }
import makeTimeout.short

private[round] final class Rematcher(
    messenger: Messenger,
    onStart: String => Unit,
    rematch960Cache: ExpireSetMemo,
    isRematchCache: ExpireSetMemo) {

  def yes(pov: Pov)(implicit proxy: GameProxy): Fu[Events] = pov match {
    case Pov(game, color) if (game playerCanRematch color) =>
      (game.opponent(color).isOfferingRematch || game.opponent(color).isAi).fold(
        game.next.fold(rematchJoin(pov))(rematchExists(pov)),
        rematchCreate(pov)
      )
    case _ => fuccess(Nil)
  }

  def no(pov: Pov)(implicit proxy: GameProxy): Fu[Events] = pov match {
    case Pov(game, color) if pov.player.isOfferingRematch => proxy.save {
      messenger.system(game, _.rematchOfferCanceled)
      Progress(game) map { g => g.updatePlayer(color, _.removeRematchOffer) }
    } inject List(Event.ReloadOwner)
    case Pov(game, color) if pov.opponent.isOfferingRematch => proxy.save {
      messenger.system(game, _.rematchOfferDeclined)
      Progress(game) map { g => g.updatePlayer(!color, _.removeRematchOffer) }
    } inject List(Event.ReloadOwner)
    case _ => fuccess(Nil)
  }

  private def rematchExists(pov: Pov)(nextId: String): Fu[Events] =
    GameRepo game nextId flatMap {
      _.fold(rematchJoin(pov))(g => fuccess(redirectEvents(g)))
    }

  private def rematchJoin(pov: Pov): Fu[Events] = for {
    nextGame ← returnGame(pov) map (_.start)
    _ ← (GameRepo insertDenormalized nextGame) >>
      GameRepo.saveNext(pov.game, nextGame.id) >>-
      messenger.system(pov.game, _.rematchOfferAccepted) >>- {
        isRematchCache.put(nextGame.id)
        if (pov.game.variant == Chess960 && !rematch960Cache.get(pov.game.id))
          rematch960Cache.put(nextGame.id)
      }
  } yield {
    onStart(nextGame.id)
    redirectEvents(nextGame)
  }

  private def rematchCreate(pov: Pov)(implicit proxy: GameProxy): Fu[Events] = proxy.save {
    messenger.system(pov.game, _.rematchOfferSent)
    Progress(pov.game) map { g => g.updatePlayer(pov.color, _ offerRematch) }
  } inject List(Event.ReloadOwner)

  private def returnGame(pov: Pov): Fu[Game] = for {
    initialFen <- GameRepo initialFen pov.game
    situation = initialFen flatMap Forsyth.<<<
    pieces = pov.game.variant match {
      case Chess960 =>
        if (rematch960Cache.get(pov.game.id)) Chess960.pieces
        else situation.fold(Chess960.pieces)(_.situation.board.pieces)
      case FromPosition => situation.fold(Standard.pieces)(_.situation.board.pieces)
      case variant      => variant.pieces
    }
    users <- UserRepo byIds pov.game.userIds
  } yield Game.make(
    game = ChessGame(
      board = Board(pieces, variant = pov.game.variant).withCastles {
        situation.fold(Castles.init)(_.situation.board.history.castles)
      },
      clock = pov.game.clock map (_.reset),
      turns = situation ?? (_.turns),
      startedAtTurn = situation ?? (_.turns)),
    whitePlayer = returnPlayer(pov.game, White, users),
    blackPlayer = returnPlayer(pov.game, Black, users),
    mode = if (users.exists(_.lame)) chess.Mode.Casual else pov.game.mode,
    variant = pov.game.variant,
    source = pov.game.source | Source.Lobby,
    daysPerTurn = pov.game.daysPerTurn,
    pgnImport = None)

  private def returnPlayer(game: Game, color: ChessColor, users: List[User]): lila.game.Player = {
    val player = lila.game.Player.make(color = color, aiLevel = game.opponent(color).aiLevel)
    game.player(!color).userId.flatMap { id =>
      users.find(_.id == id)
    }.fold(player) { user =>
      player.withUser(user.id, PerfPicker.mainOrDefault(game)(user.perfs))
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
