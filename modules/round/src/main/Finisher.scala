package lila.round

import chess.Color._
import chess.Status._
import chess.{ Status, Color, Speed, Variant }

import lila.db.api._
import lila.game.tube.gameTube
import lila.game.{ GameRepo, Game, Pov, Event }
import lila.hub.actorApi.round._
import lila.i18n.I18nKey.{ Select ⇒ SelectI18nKey }
import lila.user.tube.userTube
import lila.user.{ User, UserRepo }

private[round] final class Finisher(
    tournamentOrganizer: akka.actor.ActorSelection,
    messenger: Messenger,
    indexer: akka.actor.ActorSelection) {

  def apply(
    game: Game,
    status: Status.type ⇒ Status,
    winner: Option[Color] = None,
    message: Option[SelectI18nKey] = None): Fu[Events] = for {
    p1 ← fuccess {
      game.finish(status(Status), winner)
    }
    p2 ← message.fold(fuccess(p1)) { m ⇒
      messenger.systemMessage(p1.game, m) map p1.++
    }
    _ ← GameRepo save p2
    g = p2.game
    winnerId = winner flatMap (g.player(_).userId)
    _ ← GameRepo.finish(g.id, winner, winnerId) >>
      updatePerfs(g) >>-
      (indexer ! lila.game.actorApi.InsertGame(g)) >>-
      (tournamentOrganizer ! FinishGame(g.id))
  } yield p2.events

  private def updatePerfs(game: Game): Funit = ~{
    (game.player(White).userId |@| game.player(Black).userId) {
      case (uidW, uidB) ⇒ UserRepo.isEngine(uidW) zip UserRepo.isEngine(uidB) flatMap {
        case (true, _) if game.winnerColor.exists(White==) ⇒ funit
        case (_, true) if game.winnerColor.exists(Black==) ⇒ funit
        case _ ⇒ PerfsUpdater save game
      }
    }
  }
}
