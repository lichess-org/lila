package lila.gameSearch

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.util.Try

import lila.common.PimpedJson._
import lila.db.dsl._
import lila.game.{ Game, GameRepo }
import lila.search._

final class GameSearchApi(client: ESClient) extends SearchReadApi[Game, Query] {

  private var writeable = true

  def search(query: Query, from: From, size: Size) =
    client.search(query, from, size) flatMap { res =>
      GameRepo gamesFromSecondary res.ids
    }

  def count(query: Query) =
    client.count(query) map (_.count)

  def ids(query: Query, max: Int): Fu[List[String]] =
    client.search(query, From(0), Size(max)).map(_.ids)

  def store(game: Game) = (writeable && storable(game)) ?? {
    GameRepo isAnalysed game.id flatMap { analysed =>
      client.store(Id(game.id), toDoc(game, analysed))
    }
  }

  private def storable(game: Game) = (game.finished || game.imported)

  private def toDoc(game: Game, analysed: Boolean) = Json.obj(
    Fields.status -> (game.status match {
      case s if s.is(_.Timeout) => chess.Status.Resign
      case s if s.is(_.NoStart) => chess.Status.Resign
      case s => game.status
    }).id,
    Fields.turns -> math.ceil(game.turns.toFloat / 2),
    Fields.rated -> game.rated,
    Fields.perf -> game.perfType.map(_.id),
    Fields.uids -> game.userIds.toArray.some.filterNot(_.isEmpty),
    Fields.winner -> (game.winner flatMap (_.userId)),
    Fields.winnerColor -> game.winner.fold(3)(_.color.fold(1, 2)),
    Fields.averageRating -> game.averageUsersRating,
    Fields.ai -> game.aiLevel,
    Fields.date -> (lila.search.Date.formatter print game.updatedAtOrCreatedAt),
    Fields.duration -> game.durationSeconds,
    Fields.clockInit -> game.clock.map(_.limitSeconds),
    Fields.clockInc -> game.clock.map(_.incrementSeconds),
    Fields.analysed -> analysed,
    Fields.whiteUser -> game.whitePlayer.userId,
    Fields.blackUser -> game.blackPlayer.userId,
    Fields.source -> game.source.map(_.id)
  ).noNull

  def indexAll: Funit = {
    writeable = false
    Thread sleep 3000
    client match {
      case c: ESClientHttp =>
        logger.info(s"Drop ${c.index.name}")
        writeable = false
        Thread sleep 3000
        c.putMapping >> indexSince("2011-01-01")
      case _ => funit
    }
  }

  def indexSince(sinceStr: String): Funit =
    parseDate(sinceStr).fold(fufail[Unit](s"Invalid date $sinceStr")) { since =>
      client match {
        case c: ESClientHttp =>
          logger.info(s"Index to ${c.index.name} since $since")
          writeable = false
          Thread sleep 3000
          doIndex(c, since) >>- {
            logger.info("[game search] Completed indexation.")
            Thread sleep 3000
            writeable = true
          }
        case _ => funit
      }
    }

  private val datePattern = "yyyy-MM-dd"
  private val dateFormatter = DateTimeFormat forPattern datePattern
  private val dateTimeFormatter = DateTimeFormat forPattern s"$datePattern HH:mm"

  private def parseDate(str: String): Option[DateTime] =
    Try(dateFormatter parseDateTime str).toOption

  private def doIndex(client: ESClientHttp, since: DateTime): Funit = {
    import lila.db.BSON.BSONJodaDateTimeHandler
    import reactivemongo.api._
    import reactivemongo.bson._
    import reactivemongo.play.iteratees.cursorProducer

    val batchSize = 1000
    val maxGames = Int.MaxValue
    // val maxGames = 10 * 1000 * 1000

    GameRepo.sortedCursor(
      selector = $doc("ca" $gt since),
      sort = $doc("ca" -> 1),
      readPreference = ReadPreference.secondaryPreferred
    )
      .enumerator(maxGames) &>
      Enumeratee.grouped(Iteratee takeUpTo batchSize) |>>>
      Enumeratee.mapM[Seq[Game]].apply[(Seq[Game], Set[String])] { games =>
        GameRepo filterAnalysed games.map(_.id) map games. ->
      } &>
      Iteratee.foldM[(Seq[Game], Set[String]), Long](nowMillis) {
        case (millis, (games, analysedIds)) =>
          client.storeBulk(games map { g =>
            Id(g.id) -> toDoc(g, analysedIds(g.id))
          }) inject {
            val date = games.headOption.map(_.createdAt) ?? dateTimeFormatter.print
            val gameMs = (nowMillis - millis) / batchSize.toDouble
            logger.info(s"$date ${(1000 / gameMs).toInt} games/s")
            nowMillis
          }
      } void
  } >> client.refresh
}
