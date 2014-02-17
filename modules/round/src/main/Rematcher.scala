package lila.round

import akka.actor.ActorSelection
import akka.pattern.ask
import chess.format.Forsyth
import chess.{ Game => ChessGame, Board, Clock, Variant, Color => ChessColor }
import ChessColor.{ White, Black }

import lila.db.api._
import lila.game.tube.gameTube
import lila.game.{ GameRepo, Game, Event, Progress, Pov, Source, AnonCookie }
import lila.memo.ExpireSetMemo
import lila.user.UserRepo
import makeTimeout.short

private[round] final class Rematcher(
    messenger: Messenger,
    router: ActorSelection,
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
    } inject List(Event.ReloadTablesOwner)
    case Pov(game, color) if pov.opponent.isOfferingRematch => GameRepo save {
      messenger.system(game, _.rematchOfferDeclined)
      Progress(game) map { g => g.updatePlayer(!color, _.removeRematchOffer) }
    } inject List(Event.ReloadTablesOwner)
    case _ => ClientErrorException.future("[rematcher] invalid no " + pov)
  }

  private def rematchExists(pov: Pov)(nextId: String): Fu[Events] =
    GameRepo game nextId flatMap {
      _.fold(rematchJoin(pov))(redirectEvents)
    }

  private def rematchJoin(pov: Pov): Fu[Events] = for {
    nextGame ← returnGame(pov) map (_.start)
    nextId = nextGame.id
    _ ← (GameRepo insertDenormalized nextGame) >>
      GameRepo.saveNext(pov.game, nextGame.id) >>-
      messenger.system(pov.game, _.rematchOfferAccepted) >>- {
        if (pov.game.variant == Variant.Chess960 && !rematch960Cache.get(pov.game.id))
          rematch960Cache.put(nextId)
      }
    events ← redirectEvents(nextGame)
  } yield events

  private def rematchCreate(pov: Pov): Fu[Events] = GameRepo save {
    messenger.system(pov.game, _.rematchOfferSent)
    Progress(pov.game) map { g => g.updatePlayer(pov.color, _ offerRematch) }
  } inject List(Event.ReloadTablesOwner)

  private def returnGame(pov: Pov): Fu[Game] = for {
    pieces ← pov.game.variant.standard.fold(
      fuccess(pov.game.variant.pieces),
      rematch960Cache.get(pov.game.id).fold(
        fuccess(Variant.Chess960.pieces),
        GameRepo initialFen pov.game.id map { fenOption =>
          (fenOption flatMap Forsyth.<< map { _.board.pieces }) | pov.game.variant.pieces
        }
      )
    )
    whitePlayer ← returnPlayer(pov.game, White)
    blackPlayer ← returnPlayer(pov.game, Black)
  } yield Game.make(
    game = ChessGame(
      board = Board(pieces, variant = pov.game.variant),
      clock = pov.game.clock map (_.reset)),
    whitePlayer = whitePlayer,
    blackPlayer = blackPlayer,
    mode = pov.game.mode,
    variant = pov.game.variant,
    source = pov.game.source | Source.Lobby,
    pgnImport = None)

  private def returnPlayer(game: Game, color: ChessColor): Fu[lila.game.Player] =
    lila.game.Player.make(color = color, aiLevel = game.opponent(color).aiLevel) |> { player =>
      game.player(!color).userId.fold(fuccess(player)) { userId =>
        UserRepo byId userId map { _.fold(player)(player.withUser) }
      }
    }

  private def redirectEvents(game: Game): Fu[Events] =
    router ? lila.hub.actorApi.router.Player(game fullIdOf White) zip
      router ? lila.hub.actorApi.router.Player(game fullIdOf Black) collect {
        case (whiteUrl: String, blackUrl: String) => List(
          Event.RedirectOwner(White, blackUrl, AnonCookie.json(game, Black)),
          Event.RedirectOwner(Black, whiteUrl, AnonCookie.json(game, White)),
          // tell spectators to reload the table
          Event.ReloadTable(White),
          Event.ReloadTable(Black))
      }
}
