package lila.round

import chess.{ Game ⇒ ChessGame, Board, Clock, Variant, Color ⇒ ChessColor }
import ChessColor.{ White, Black }
import chess.format.Forsyth
import lila.game.{ GameRepo, Game, Event, Progress, Pov, PlayerRef, Namer, Source }
import lila.user.User
import lila.hub.actorApi.router.Player
import makeTimeout.short

import lila.game.tube.gameTube
import lila.user.tube.userTube
import lila.db.api._

import akka.pattern.ask

private[round] final class Rematcher(
    messenger: Messenger,
    router: lila.hub.ActorLazyRef,
    timeline: lila.hub.ActorLazyRef) {

  def yes(pov: Pov): Fu[Events] = pov match {
    case Pov(game, color) if (game playerCanRematch color) ⇒
      game.opponent(color).isOfferingRematch.fold(
        game.next.fold(rematchJoin(pov))(rematchExists(pov)),
        rematchCreate(pov)
      )
    case _ ⇒ fufail("[rematcher] invalid yes " + pov)
  }

  def no(pov: Pov): Fu[Events] = pov match {
    case Pov(g1, color) if pov.player.isOfferingRematch ⇒ for {
      p1 ← messenger.systemMessage(g1, _.rematchOfferCanceled) map { es ⇒
        Progress(g1, Event.ReloadTablesOwner :: es)
      }
      p2 = p1 map { g ⇒ g.updatePlayer(color, _.removeRematchOffer) }
      _ ← GameRepo save p2
    } yield p2.events
    case Pov(g1, color) if pov.opponent.isOfferingRematch ⇒ for {
      p1 ← messenger.systemMessage(g1, _.rematchOfferDeclined) map { es ⇒
        Progress(g1, Event.ReloadTablesOwner :: es)
      }
      p2 = p1 map { g ⇒ g.updatePlayer(!color, _.removeRematchOffer) }
      _ ← GameRepo save p2
    } yield p2.events
    case _ ⇒ fufail("[rematcher] invalid no " + pov)
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
      (timeline ! nextGame) >>
      // messenges are not sent to the next game socket
      // as nobody is there to see them yet
      messenger.rematch(pov.game, nextGame)
    events ← redirectEvents(nextGame)
  } yield events

  private def rematchCreate(pov: Pov): Fu[Events] = for {
    p1 ← messenger.systemMessage(pov.game, _.rematchOfferSent) map { es ⇒
      Progress(pov.game, Event.ReloadTablesOwner :: es)
    }
    p2 = p1 map { g ⇒ g.updatePlayer(pov.color, _ offerRematch) }
    _ ← GameRepo save p2
  } yield p2.events

  private def returnGame(pov: Pov): Fu[Game] = for {
    pieces ← pov.game.variant.standard.fold(
      fuccess(pov.game.variant.pieces),
      pov.game.is960Rematch.fold(
        fuccess(Variant.Chess960.pieces),
        GameRepo initialFen pov.game.id map { fenOption ⇒
          (fenOption flatMap Forsyth.<< map { situation ⇒
            situation.board.pieces
          }) | pov.game.variant.pieces
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
    ai = None,
    creatorColor = !pov.color,
    mode = pov.game.mode,
    variant = pov.game.variant,
    source = pov.game.source | Source.Lobby,
    pgnImport = None) with960Rematch !pov.game.is960Rematch

  private def returnPlayer(game: Game, color: ChessColor): Fu[lila.game.Player] =
    lila.game.Player.make(color = color, aiLevel = None) |> { player ⇒
      game.player(!color).userId.fold(fuccess(player)) { userId ⇒
        $find.byId[User](userId) map { _.fold(player)(player.withUser) }
      }
    }

  private def redirectEvents(nextGame: Game): Fu[Events] =
    router ? Player(nextGame fullIdOf White) zip
      router ? Player(nextGame fullIdOf Black) collect {
        case (whiteUrl: String, blackUrl: String) ⇒ List(
          Event.RedirectOwner(White, blackUrl),
          Event.RedirectOwner(Black, whiteUrl),
          // tell spectators to reload the table
          Event.ReloadTable(White),
          Event.ReloadTable(Black))
      }
}
