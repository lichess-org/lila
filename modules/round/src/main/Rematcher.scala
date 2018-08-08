package lidraughts.round

import draughts.format.Forsyth
import draughts.variant._
import draughts.{ DraughtsGame, Board, Color => DraughtsColor, Clock, Situation }
import DraughtsColor.{ White, Black }
import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import scala.concurrent.duration._

import lidraughts.game.{ GameRepo, Game, Event, Progress, Pov, Source, AnonCookie, PerfPicker }
import lidraughts.memo.ExpireSetMemo
import lidraughts.user.{ User, UserRepo }

private[round] final class Rematcher(
    messenger: Messenger,
    onStart: String => Unit,
    rematch960Cache: ExpireSetMemo
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
            //if (pov.game.variant == Chess960 && !rematch960Cache.get(pov.game.id))
            //  rematch960Cache.put(nextGame.id)
          }
      } yield {
        onStart(nextGame.id)
        redirectEvents(nextGame)
      }
      case Some(rematchId) => GameRepo game rematchId map { _ ?? redirectEvents }
    }

  private def rematchCreate(pov: Pov)(implicit proxy: GameProxy): Fu[Events] = proxy.save {
    messenger.system(pov.game, _.rematchOfferSent)
    Progress(pov.game) map { g => g.updatePlayer(pov.color, _ offerRematch) }
  } inject List(Event.RematchOffer(by = pov.color.some))

  private def returnGame(pov: Pov): Fu[Game] = for {
    initialFen <- GameRepo initialFen pov.game
    situation = initialFen flatMap Forsyth.<<<
    pieces = pov.game.variant match {
      /*case Chess960 =>
        if (rematch960Cache.get(pov.game.id)) Chess960.pieces
        else situation.fold(Chess960.pieces)(_.situation.board.pieces)*/
      case FromPosition => situation.fold(Standard.pieces)(_.situation.board.pieces)
      case variant => variant.pieces
    }
    users <- UserRepo byIds pov.game.userIds
  } yield Game.make(
    draughts = DraughtsGame(
      situation = Situation(
        board = Board(pieces, variant = pov.game.variant),
        color = situation.fold[draughts.Color](White)(_.situation.color)
      ),
      clock = pov.game.clock map { c => Clock(c.config) },
      turns = situation ?? (_.turns),
      startedAtTurn = situation ?? (_.turns)
    ),
    whitePlayer = returnPlayer(pov.game, White, users),
    blackPlayer = returnPlayer(pov.game, Black, users),
    mode = if (users.exists(_.lame)) draughts.Mode.Casual else pov.game.mode,
    source = pov.game.source | Source.Lobby,
    daysPerTurn = pov.game.daysPerTurn,
    pdnImport = None
  )

  private def returnPlayer(game: Game, color: DraughtsColor, users: List[User]): lidraughts.game.Player =
    game.opponent(color).aiLevel match {
      case Some(ai) => lidraughts.game.Player.make(color, ai.some)
      case None => lidraughts.game.Player.make(
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
