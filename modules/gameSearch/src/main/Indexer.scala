package lila.gameSearch

import akka.actor._
import akka.pattern.pipe
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mapping.FieldType._

import lila.game.actorApi.{ InsertGame, FinishGame }
import lila.search.actorApi._
import lila.search.ElasticSearch

private[gameSearch] final class Indexer(
    client: ElasticClient,
    indexName: String,
    typeName: String,
    analyser: lila.analyse.Analyser) extends Actor {

  context.system.lilaBus.subscribe(self, 'finishGame)

  private val indexType = s"$indexName/$typeName"

  def receive = {

    case Search(definition)     => client execute definition pipeTo sender
    case Count(definition)      => client execute definition pipeTo sender

    case FinishGame(game, _, _) => self ! InsertGame(game)

    case InsertGame(game) => if (storable(game)) {
      analyser hasDone game.id foreach { analysed =>
        client execute store(game, analysed)
      }
    }

    case Reset =>
      ElasticSearch.createType(client, indexName, typeName)
      try {
        import Fields._
        client.putMapping(indexName) {
          typeName as (
            status typed ShortType,
            turns typed ShortType,
            rated typed BooleanType,
            variant typed ShortType,
            uids typed StringType,
            winner typed StringType,
            averageRating typed ShortType,
            ai typed ShortType,
            opening typed StringType,
            date typed DateType format ElasticSearch.Date.format,
            duration typed ShortType,
            analysed typed BooleanType
          )
        }
        import scala.concurrent.Await
        import scala.concurrent.duration._
        import play.api.libs.json.Json
        import lila.db.api._
        import lila.game.tube.gameTube
        loginfo("[game search] counting games...")
        val size = $count($select.all).await
        val batchSize = 1000
        var nb = 0
        var nbSkipped = 0
        var started = nowMillis
        Await.result(
          $enumerate.bulk[Option[lila.game.Game]]($query.all, batchSize) { gameOptions =>
            val games = gameOptions.flatten filter storable
            val nbGames = games.size
            (analyser hasMany games.map(_.id).toSeq flatMap { analysedIds =>
              client bulk {
                games.map { g => store(g, analysedIds(g.id)) }: _*
              }
            }).void >>- {
              nb = nb + nbGames
              nbSkipped = nbSkipped + gameOptions.size - nbGames
              val perS = (batchSize * 1000) / math.max(1, (nowMillis - started))
              started = nowMillis
              loginfo("[game search] Indexed %d of %d, skipped %d, at %d/s".format(nb, size, nbSkipped, perS))
            }
          },
          10 hours)
        sender ! ()
      }
      catch {
        case e: Exception =>
          println(e)
          sender ! Status.Failure(e)
      }
  }

  private def storable(game: lila.game.Game) =
    (game.finished || game.imported) && game.playedTurns > 4 && game.players.forall {
      _.rating.fold(true)(_ >= 1000)
    }

  private def store(game: lila.game.Game, hasAnalyse: Boolean) = {
    import Fields._
    index into indexType fields {
      List(
        status -> game.status.is(_.Timeout).fold(chess.Status.Resign, game.status).id.some,
        turns -> math.ceil(game.turns.toFloat / 2).some,
        rated -> game.rated.some,
        variant -> game.variant.id.some,
        uids -> game.userIds.toArray.some.filterNot(_.isEmpty),
        winner -> (game.winner flatMap (_.userId)),
        averageRating -> game.averageUsersRating,
        ai -> game.aiLevel,
        date -> (ElasticSearch.Date.formatter print game.createdAt).some,
        duration -> game.estimateTotalTime.some,
        opening -> (chess.OpeningExplorer openingOf game.pgnMoves map (_.code.toLowerCase)),
        analysed -> hasAnalyse.some
      ).collect {
          case (key, Some(value)) => key -> value
        }: _*
    } id game.id
  }
}
