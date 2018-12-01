package lidraughts.gameSearch

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.concurrent.duration._
import scala.util.Try

import lidraughts.db.dsl._
import lidraughts.game.{ Game, GameRepo }
import lidraughts.search._

final class GameSearchApi(
    client: ESClient,
    system: akka.actor.ActorSystem
) extends SearchReadApi[Game, Query] {

  def search(query: Query, from: From, size: Size) =
    client.search(query, from, size) flatMap { res =>
      GameRepo gamesFromSecondary res.ids
    }

  def count(query: Query) =
    client.count(query) map (_.count)

  def ids(query: Query, max: Int): Fu[List[String]] =
    client.search(query, From(0), Size(max)).map(_.ids)

  def store(game: Game) = storable(game) ?? {
    GameRepo isAnalysed game.id flatMap { analysed =>
      lidraughts.common.Future.retry(
        () => client.store(Id(game.id), toDoc(game, analysed)),
        delay = 10.seconds,
        retries = 5,
        logger.some
      )(system)
    }
  }

  import reactivemongo.play.iteratees.cursorProducer

  def reset = client match {
    case c: ESClientHttp => c.putMapping >> {
      import play.api.libs.iteratee._
      import reactivemongo.api.ReadPreference
      import lidraughts.db.dsl._
      logger.info(s"Index to ${c.index.name}")
      val batchSize = 500
      val maxEntries = Int.MaxValue
      GameRepo.cursor(
        selector = $empty,
        readPreference = ReadPreference.secondaryPreferred
      )
        .enumerator(maxEntries) &>
        Enumeratee.grouped(Iteratee takeUpTo batchSize) |>>>
        Iteratee.foldM[Seq[Game], Int](0) {
          case (nb, games) => for {
            _ <- c.storeBulk(games map (g => Id(g.id) -> toDoc(g, false)))
          } yield {
            logger.info(s"Indexed $nb games")
            nb + games.size
          }
        }
    } >> client.refresh

    case _ => funit
  }

  private def storable(game: Game) = game.finished || game.imported

  private def toDoc(game: Game, analysed: Boolean) = Json.obj(
    Fields.status -> (game.status match {
      case s if s.is(_.Timeout) => draughts.Status.Resign
      case s if s.is(_.NoStart) => draughts.Status.Resign
      case s => game.status
    }).id,
    Fields.turns -> math.ceil(game.turns.toFloat / 2),
    Fields.rated -> game.rated,
    Fields.perf -> game.perfType.map(_.id),
    Fields.uids -> game.userIds.toArray.some.filterNot(_.isEmpty),
    Fields.winner -> game.winner.flatMap(_.userId),
    Fields.loser -> game.loser.flatMap(_.userId),
    Fields.winnerColor -> game.winner.fold(3)(_.color.fold(1, 2)),
    Fields.averageRating -> game.averageUsersRating,
    Fields.ai -> game.aiLevel,
    Fields.date -> (lidraughts.search.Date.formatter print game.movedAt),
    Fields.duration -> game.durationSeconds, // for realtime games only
    Fields.clockInit -> game.clock.map(_.limitSeconds),
    Fields.clockInc -> game.clock.map(_.incrementSeconds),
    Fields.analysed -> analysed,
    Fields.whiteUser -> game.whitePlayer.userId,
    Fields.blackUser -> game.blackPlayer.userId,
    Fields.source -> game.source.map(_.id)
  ).noNull
}
