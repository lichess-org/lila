package lidraughts.api

import org.joda.time.DateTime
import play.api.libs.iteratee._
import scala.concurrent.duration._

import draughts.format.pdn.Pdn
import lidraughts.common.MaxPerSecond
import lidraughts.game.Game
import lidraughts.game.PdnDump.WithFlags
import lidraughts.game.{ GameRepo, Query }

final class PdnDump(
    dumper: lidraughts.game.PdnDump,
    getSimulName: String => Fu[Option[String]],
    getTournamentName: String => Option[String]
)(implicit system: akka.actor.ActorSystem) {

  import PdnDump._

  def apply(game: Game, initialFen: Option[String], flags: WithFlags): Fu[Pdn] = {
    val pdn = dumper(game, initialFen, flags)
    if (flags.tags) (game.simulId ?? getSimulName) map { simulName =>
      simulName.orElse(game.tournamentId flatMap getTournamentName).fold(pdn)(pdn.withEvent)
    }
    else fuccess(pdn)
  }

  def filename(game: Game) = dumper filename game

  private def toPdn(flags: WithFlags) =
    Enumeratee.mapM[Game].apply[String] { game =>
      GameRepo initialFen game flatMap { initialFen =>
        apply(game, initialFen, flags).map(pdn => s"$pdn\n\n\n")
      }
    }

  def exportUserGames(config: Config): Enumerator[String] = {
    import reactivemongo.play.iteratees.cursorProducer
    import lidraughts.db.dsl._
    GameRepo.sortedCursor(
      Query.user(config.user.id) ++ Query.createdBetween(config.since, config.until),
      Query.sortCreated,
      batchSize = config.perSecond.value
    ).bulkEnumerator(maxDocs = config.max | Int.MaxValue) &>
      lidraughts.common.Iteratee.delay(1 second) &>
      Enumeratee.mapConcat(_.filter(config.postFilter).toSeq) &>
      toPdn(config.flags)
  }

  // def exportGamesFromIds(ids: List[String], draughtsResult: Boolean): Enumerator[String] =
  //   Enumerator.enumerate(ids grouped 50) &>
  //     Enumeratee.mapM[List[String]].apply[List[Game]](GameRepo.gamesFromSecondary) &>
  //     Enumeratee.mapConcat(identity) &>
  //     toPdn(WithFlags(draughtsResult = draughtsResult))
}

object PdnDump {

  case class Config(
      user: lidraughts.user.User,
      since: Option[DateTime] = None,
      until: Option[DateTime] = None,
      max: Option[Int] = None,
      rated: Option[Boolean] = None,
      perfType: Set[lidraughts.rating.PerfType],
      color: Option[draughts.Color],
      flags: WithFlags,
      perSecond: MaxPerSecond
  ) {
    def postFilter(g: Game) =
      rated.fold(true)(g.rated ==) && {
        perfType.isEmpty || g.perfType.exists(perfType.contains)
      } && color.fold(true) { c =>
        g.player(c).userId has user.id
      }
  }
}
