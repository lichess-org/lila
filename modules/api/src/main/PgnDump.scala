package lila.api

import org.joda.time.DateTime
import play.api.libs.iteratee._
import scala.concurrent.duration._

import chess.format.pgn.Pgn
import lila.common.MaxPerSecond
import lila.game.Game
import lila.game.PgnDump.WithFlags
import lila.game.{ GameRepo, Query }

final class PgnDump(
    dumper: lila.game.PgnDump,
    getSimulName: String => Fu[Option[String]],
    getTournamentName: String => Option[String],
    system: akka.actor.ActorSystem
) {

  import PgnDump._

  def apply(game: Game, initialFen: Option[String], flags: WithFlags): Fu[Pgn] =
    (game.simulId ?? getSimulName) map { simulName =>
      val pgn = dumper(game, initialFen, flags)
      simulName.orElse(game.tournamentId flatMap getTournamentName).fold(pgn)(pgn.withEvent)
    }

  def filename(game: Game) = dumper filename game

  private def toPgn(flags: WithFlags) =
    Enumeratee.mapM[Game].apply[String] { game =>
      GameRepo initialFen game flatMap { initialFen =>
        apply(game, initialFen, flags).map(pgn => s"$pgn\n\n\n")
      }
    }

  private def delay[A](duration: FiniteDuration): Enumeratee[A, A] =
    Enumeratee.mapM[A].apply[A] { as =>
      lila.common.Future.delay[A](duration)(fuccess(as))(system)
    }

  private def throttle[A]: Enumeratee[Iterator[A], A] =
    delay[Iterator[A]](1 second) ><>
      Enumeratee.mapConcat[Iterator[A]].apply[A](_.toSeq)

  def exportUserGames(config: Config): Enumerator[String] = {
    import reactivemongo.play.iteratees.cursorProducer
    import lila.db.dsl._
    GameRepo.sortedCursor(
      Query.user(config.user.id) ++ Query.createdBetween(config.since, config.until),
      Query.sortCreated,
      batchSize = config.perSecond.value
    ).bulkEnumerator(maxDocs = config.max | Int.MaxValue) &> throttle &> toPgn(config.flags)
  }

  def exportGamesFromIds(ids: List[String]): Enumerator[String] =
    Enumerator.enumerate(ids grouped 50) &>
      Enumeratee.mapM[List[String]].apply[List[Game]](GameRepo.gamesFromSecondary) &>
      Enumeratee.mapConcat(identity) &>
      toPgn(WithFlags())
}

object PgnDump {

  case class Config(
      user: lila.user.User,
      since: Option[DateTime] = None,
      until: Option[DateTime] = None,
      max: Option[Int] = None,
      flags: WithFlags,
      perSecond: MaxPerSecond
  )
}
