package lila.round

import chess.format.Forsyth
import chess.variant._
import chess.{ Game => ChessGame, Board, Color => ChessColor, Castles, Clock, Situation }
import ChessColor.{ White, Black }
import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import scala.concurrent.duration._

import lila.game.{ GameRepo, Game, Event, Progress, Pov, Source, AnonCookie, PerfPicker }
import lila.memo.ExpireSetMemo
import lila.user.{ User, UserRepo }

private[round] final class Rematcher(
    messenger: Messenger,
    onStart: String => Unit,
    rematch960Cache: ExpireSetMemo,
    bus: lila.common.Bus
) {

  private val rematchCreated: Cache[Game.ID, Game.ID] = Scaffeine()
    .expireAfterWrite(1 minute)
    .build[Game.ID, Game.ID]

  def yes(pov: Pov)(implicit proxy: GameProxy): Fu[Events] = pov match {
    case Pov(game, color) if game playerCanRematch color =>
      if (game.opponent(color).isOfferingRematch || game.opponent(color).isAi)
        game.next.fold(rematchJoin(pov))(rematchExists(pov))
      else rematchCreate(pov)
    case _ => fuccess(List(Event.ReloadOwner))
  }

  def no(pov: Pov)(implicit proxy: GameProxy): Fu[Events] = pov match {
    case Pov(game, color) if pov.player.isOfferingRematch => proxy.save {
      messenger.system(game, _.rematchOfferCanceled)
      Progress(game) map { g => g.updatePlayer(color, _.removeRematchOffer) }
    } inject List(Event.RematchOffer(by = none))
    case Pov(game, color) if pov.opponent.isOfferingRematch => proxy.save {
      messenger.system(game, _.rematchOfferDeclined)
      Progress(game) map { g => g.updatePlayer(!color, _.removeRematchOffer) }
    } inject List(Event.RematchOffer(by = none))
    case _ => fuccess(List(Event.ReloadOwner))
  }

  private def rematchExists(pov: Pov)(nextId: String): Fu[Events] =
    GameRepo game nextId flatMap {
      _.fold(rematchJoin(pov))(g => fuccess(redirectEvents(g)))
    }

  private def rematchJoin(pov: Pov): Fu[Events] =
    rematchCreated.getIfPresent(pov.gameId) match {
      case None => for {
        nextGame ← returnGame(pov) map (_.start)
        _ = rematchCreated.put(pov.gameId, nextGame.id)
        _ ← (GameRepo insertDenormalized nextGame) >>
          GameRepo.saveNext(pov.game, nextGame.id) >>-
          messenger.system(pov.game, _.rematchOfferAccepted) >>- {
            if (pov.game.variant == Chess960 && !rematch960Cache.get(pov.gameId))
              rematch960Cache.put(nextGame.id)
          }
      } yield {
        onStart(nextGame.id)
        redirectEvents(nextGame)
      }
      case Some(rematchId) => GameRepo game rematchId map { _ ?? redirectEvents }
    }

  private def rematchCreate(pov: Pov)(implicit proxy: GameProxy): Fu[Events] = proxy.save {
    messenger.system(pov.game, _.rematchOfferSent)
    pov.opponent.userId foreach { forId =>
      bus.publish(lila.hub.actorApi.round.RematchOffer(pov.gameId), Symbol(s"rematchFor:$forId"))
    }
    Progress(pov.game) map { g => g.updatePlayer(pov.color, _ offerRematch) }
  } inject List(Event.RematchOffer(by = pov.color.some))

  private def returnGame(pov: Pov): Fu[Game] = for {
    initialFen <- GameRepo initialFen pov.game
    situation = initialFen flatMap { fen => Forsyth <<< fen.value }
    pieces = pov.game.variant match {
      case Chess960 =>
        if (rematch960Cache get pov.gameId) Chess960.pieces
        else situation.fold(Chess960.pieces)(_.situation.board.pieces)
      case FromPosition => situation.fold(Standard.pieces)(_.situation.board.pieces)
      case variant => variant.pieces
    }
    users <- UserRepo byIds pov.game.userIds
    game <- Game.make(
      chess = ChessGame(
        situation = Situation(
          board = Board(pieces, variant = pov.game.variant).withCastles {
            situation.fold(Castles.init)(_.situation.board.history.castles)
          },
          color = situation.fold[chess.Color](White)(_.situation.color)
        ),
        clock = pov.game.clock map { c => Clock(c.config) },
        turns = situation ?? (_.turns),
        startedAtTurn = situation ?? (_.turns)
      ),
      whitePlayer = returnPlayer(pov.game, White, users),
      blackPlayer = returnPlayer(pov.game, Black, users),
      mode = if (users.exists(_.lame)) chess.Mode.Casual else pov.game.mode,
      source = pov.game.source | Source.Lobby,
      daysPerTurn = pov.game.daysPerTurn,
      pgnImport = None
    ).withUniqueId
  } yield game

  private def returnPlayer(game: Game, color: ChessColor, users: List[User]): lila.game.Player =
    game.opponent(color).aiLevel match {
      case Some(ai) => lila.game.Player.make(color, ai.some)
      case None => lila.game.Player.make(
        color,
        game.opponent(color).userId.flatMap { id => users.find(_.id == id) },
        PerfPicker.mainOrDefault(game)
      )
    }

  private def redirectEvents(game: Game): Events = {
    val whiteId = game fullIdOf White
    val blackId = game fullIdOf Black
    List(
      Event.RedirectOwner(White, blackId, AnonCookie.json(game, Black)),
      Event.RedirectOwner(Black, whiteId, AnonCookie.json(game, White)),
      // tell spectators about the rematch
      Event.RematchTaken(game.id)
    )
  }
}
