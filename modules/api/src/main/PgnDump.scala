package lila.api

import org.joda.time.DateTime
import play.api.libs.iteratee._
import scala.concurrent.duration._

import chess.format.pgn.Pgn
import lila.analyse.{ Analysis, AnalysisRepo, Annotator }
import lila.common.MaxPerSecond
import lila.game.PgnDump.WithFlags
import lila.game.{ Game, GameRepo, Query }

final class PgnDump(
    dumper: lila.game.PgnDump,
    getSimulName: String => Fu[Option[String]],
    getTournamentName: String => Option[String]
)(implicit system: akka.actor.ActorSystem) {

  import PgnDump._

  def apply(game: Game, initialFen: Option[String], analysis: Option[Analysis], flags: WithFlags): Fu[Pgn] = {
    val pgn = dumper(game, initialFen, flags)
    if (flags.tags) (game.simulId ?? getSimulName) map { simulName =>
      simulName.orElse(game.tournamentId flatMap getTournamentName).fold(pgn)(pgn.withEvent)
    }
    else fuccess(pgn)
  } map { pgn =>
    analysis.ifTrue(flags.evals).fold(pgn)(addEvals(pgn, _))
  }

  private def addEvals(p: Pgn, analysis: Analysis): Pgn = analysis.infos.foldLeft(p) {
    case (pgn, info) => pgn.updateTurn(info.turn, turn =>
      turn.update(info.color, move => {
        val comment = info.cp.map(_.pawns.toString)
          .orElse(info.mate.map(m => s"#${m.value}"))
        move.copy(
          comments = comment.map(c => s"[%eval $c]").toList ::: move.comments
        )
      }))
  }

  def filename(game: Game) = dumper filename game

  private def toPgn(flags: WithFlags) =
    Enumeratee.mapM[Game].apply[String] { game =>
      GameRepo initialFen game flatMap { initialFen =>
        (flags.evals ?? AnalysisRepo.byGame(game)) flatMap { analysis =>
          apply(game, initialFen, analysis, flags).map { pgn =>
            // merge analysis & eval comments
            // 1. e4 { [%eval 0.17] } { [%clk 0:00:30] }
            // 1. e4 { [%eval 0.17] [%clk 0:00:30] }
            s"$pgn\n\n\n".replace("] } { [", "] [")
          }
        }
      }
    }

  def exportUserGames(config: Config): Enumerator[String] = {
    import reactivemongo.play.iteratees.cursorProducer
    import lila.db.dsl._
    GameRepo.sortedCursor(
      Query.user(config.user.id) ++ Query.createdBetween(config.since, config.until),
      Query.sortCreated,
      batchSize = config.perSecond.value
    ).bulkEnumerator(maxDocs = config.max | Int.MaxValue) &>
      lila.common.Iteratee.delay(1 second) &>
      Enumeratee.mapConcat(_.filter(config.postFilter).toSeq) &>
      toPgn(config.flags)
  }

  // def exportGamesFromIds(ids: List[String]): Enumerator[String] =
  //   Enumerator.enumerate(ids grouped 50) &>
  //     Enumeratee.mapM[List[String]].apply[List[Game]](GameRepo.gamesFromSecondary) &>
  //     Enumeratee.mapConcat(identity) &>
  //     toPgn(WithFlags())
}

object PgnDump {

  case class Config(
      user: lila.user.User,
      since: Option[DateTime] = None,
      until: Option[DateTime] = None,
      max: Option[Int] = None,
      rated: Option[Boolean] = None,
      perfType: Set[lila.rating.PerfType],
      analysed: Option[Boolean] = None,
      color: Option[chess.Color],
      flags: WithFlags,
      perSecond: MaxPerSecond
  ) {
    def postFilter(g: Game) =
      rated.fold(true)(g.rated ==) && {
        perfType.isEmpty || g.perfType.exists(perfType.contains)
      } && color.fold(true) { c =>
        g.player(c).userId has user.id
      } && analysed.fold(true)(g.metadata.analysed ==)
  }
}
