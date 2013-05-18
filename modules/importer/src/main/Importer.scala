package lila.importer

import chess.{ Color, Move, Status }
import lila.game.{ Game, GameRepo, Pov }
import lila.round.Finisher
import lila.round.actorApi.round._
import lila.hub.actorApi.map._
import lila.game.tube.gameTube
import lila.db.api._
import makeTimeout.large

import akka.actor.ActorRef
import akka.pattern.ask
import scala.concurrent.duration.Duration

private[importer] final class Importer(
    roundMap: ActorRef,
    finisher: Finisher,
    bookmark: lila.hub.ActorLazyRef,
    delay: Duration) {

  def apply(data: ImportData, user: Option[String]): Fu[Game] = gameExists(data.pgn) {
    (data preprocess user).fold[Fu[Game]](fufail(_), {
      case Preprocessed(game, moves, result) ⇒ for {
        _ ← (GameRepo insertDenormalized game) >> applyMoves(Pov(game, Color.white), moves)
        dbGame ← $find.byId[Game](game.id)
        _ ← ~((dbGame |@| result) apply {
          case (dbg, res) ⇒ finish(dbg, res)
        }) >>- ~((dbGame |@| user) apply {
          case (dbg, u) ⇒ bookmark ! (dbg.id -> u)
        })
      } yield game
    })
  }

  private def gameExists(pgn: String)(processing: ⇒ Fu[Game]): Fu[Game] =
    $find.one(lila.game.Query pgnImport pgn) flatMap {
      _.fold(processing)(game ⇒ fuccess(game))
    }

  private def finish(game: Game, result: Result): Funit = (result match {
    case Result(Status.Draw, _) ⇒ finisher drawForce game
    case Result(Status.Resign, Some(color)) ⇒ roundMap ? Ask(
      game.id,
      Resign(game.player(!color).id)
    )
    case _ ⇒ funit
  }).void

  private def applyMoves(pov: Pov, moves: List[Move]): Funit = moves match {
    case Nil ⇒ funit
    case move :: rest ⇒ applyMove(pov, move) >>-
      (Thread sleep delay.toMillis) >>
      applyMoves(!pov, rest)
  }

  private def applyMove(pov: Pov, move: Move) = roundMap ? Ask(pov.gameId, Play(
    playerId = pov.playerId,
    orig = move.orig.toString,
    dest = move.dest.toString,
    prom = move.promotion map (_.forsyth.toString)
  )) logFailure ("[importer] apply move")
}
