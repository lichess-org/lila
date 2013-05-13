package lila.setup

import chess.{ Game ⇒ ChessGame, Board, Clock, Variant, Color ⇒ ChessColor }
import ChessColor.{ White, Black }
import chess.format.Forsyth
import lila.game.{ GameRepo, Game, Event, Progress, Pov, Handler, Namer, Source }
import lila.round.Messenger
import lila.user.User
import lila.hub.actorApi.router.Player
import makeTimeout.short

import lila.game.tube.gameTube
import lila.user.tube.userTube
import lila.db.api._

import play.api.libs.json.{ Json, JsObject }
import akka.pattern.ask

private[setup] final class Rematcher(
    messenger: Messenger,
    router: lila.hub.ActorLazyRef,
    timeline: lila.hub.ActorLazyRef) extends Handler {

  private type Result = (String, List[Event])

  def offerOrAccept(fullId: String): Fu[Result] = attempt(fullId, {
    case pov @ Pov(game, color) if game playerCanRematch color ⇒
    game.opponent(color).isOfferingRematch.fold(
      game.next.fold(rematchJoin(pov))(rematchExists(pov)),
      rematchCreate(pov)
    )
    case _ ⇒ fufail("invalid rematch offer " + fullId)
  })

  private def rematchExists(pov: Pov)(nextId: String): Fu[Result] =
    GameRepo.pov(nextId, !pov.color) map {
      _.fold(pov.fullId -> Nil)(_.fullId -> Nil)
    }

  private def rematchJoin(pov: Pov): Fu[Result] = for {
    nextGame ← returnGame(pov) map (_.start)
    nextId = nextGame.id
    _ ← (GameRepo insertDenormalized nextGame) >>
      GameRepo.saveNext(pov.game, nextGame.id) >>-
      (timeline ! nextGame) >>
      // messenges are not sent to the next game socket
      // as nobody is there to see them yet
      messenger.rematch(pov.game, nextGame)
    result ← router ? Player(nextGame fullIdOf White) zip
      router ? Player(nextGame fullIdOf Black) collect {
        case (whiteUrl: String, blackUrl: String) ⇒
          (nextGame fullIdOf !pov.color) -> List(
            Event.RedirectOwner(White, blackUrl),
            Event.RedirectOwner(Black, whiteUrl),
            // tell spectators to reload the table
            Event.ReloadTable(White),
            Event.ReloadTable(Black))
      }
  } yield result

  private def rematchCreate(pov: Pov): Fu[Result] = for {
    p1 ← messenger.systemMessage(pov.game, _.rematchOfferSent) map { es ⇒
      Progress(pov.game, Event.ReloadTable(!pov.color) :: es)
    }
    p2 = p1 map { g ⇒ g.updatePlayer(pov.color, _ offerRematch) }
    _ ← GameRepo save p2
  } yield pov.fullId -> p2.events

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
}
