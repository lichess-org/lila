package lila.gameSearch

import akka.actor._
import akka.pattern.pipe

import lila.game.actorApi.{ InsertGame, FinishGame }
import lila.game.GameRepo
import lila.search.actorApi._
import lila.search.{ ESClient, ElasticSearch }

private[gameSearch] final class Indexer(
    client: ESClient,
    indexName: String,
    typeName: String) extends Actor {

  context.system.lilaBus.subscribe(self, 'finishGame)

  def receive = {

    // case Search(definition)     => sender ! Nil
    // case Count(definition)      => sender ! 0

    // case FinishGame(game, _, _) => self ! InsertGame(game)

    // case InsertGame(game) => if (storable(game)) {
    //   GameRepo isAnalysed game.id foreach { analysed =>
    //     client store store(indexName, game, analysed)
    //   }
    // }

    case Reset =>
      sys error "Game search reset disabled"
    // val tempIndexName = "lila_" + ornicar.scalalib.Random.nextString(4)
    // client.createType(tempIndexName, typeName)
    // try {
    //   import Fields._
    //   client.put {
    //     put mapping tempIndexName / typeName as Seq(
    //       status typed ShortType,
    //       turns typed ShortType,
    //       rated typed BooleanType,
    //       variant typed ShortType,
    //       uids typed StringType,
    //       winner typed StringType,
    //       winnerColor typed ShortType,
    //       averageRating typed ShortType,
    //       ai typed ShortType,
    //       opening typed StringType,
    //       date typed DateType format ElasticSearch.Date.format,
    //       duration typed IntegerType,
    //       analysed typed BooleanType,
    //       whiteUser typed StringType,
    //       blackUser typed StringType
    //     ).map(_ index "not_analyzed")
    //   }.await
    //   import scala.concurrent.Await
    //   import scala.concurrent.duration._
    //   import play.api.libs.json.Json
    //   import lila.db.api._
    //   import lila.game.tube.gameTube
    //   loginfo("[game search] counting games...")
    //   val size = SprayPimpedFuture($count($select.all)).await
    //   val batchSize = 1000
    //   var nb = 0
    //   var nbSkipped = 0
    //   var started = nowMillis
    //   Await.result(
    //     $enumerate.bulk[Option[lila.game.Game]]($query.all, batchSize) { gameOptions =>
    //       val games = gameOptions.flatten filter storable
    //       val nbGames = games.size
    //       (GameRepo filterAnalysed games.map(_.id).toSeq flatMap { analysedIds =>
    //         client bulk {
    //           bulk {
    //             games.map { g => store(tempIndexName, g, analysedIds(g.id)) }: _*
    //           }
    //         }
    //       }) >>- {
    //         nb = nb + nbGames
    //         nbSkipped = nbSkipped + gameOptions.size - nbGames
    //         val perS = (batchSize * 1000) / math.max(1, (nowMillis - started))
    //         started = nowMillis
    //         loginfo("[game search] Indexed %d of %d, skipped %d, at %d/s".format(nb, size, nbSkipped, perS))
    //       }
    //     },
    //     10 hours)
    //   sender ! (())
    // }
    // catch {
    //   case e: Exception =>
    //     println(e)
    //     sender ! Status.Failure(e)
    // }
    // client.execute { deleteIndex(indexName) }.await
    // client.execute {
    //   add alias indexName on tempIndexName
    // }.await
  }

  // private def storable(game: lila.game.Game) =
  //   (game.finished || game.imported) && game.playedTurns > 4

  // private def store(inIndex: String, game: lila.game.Game, hasAnalyse: Boolean) = {
  //   import Fields._
  //   index into s"$inIndex/$typeName" fields {
  //     List(
  //       status -> (game.status match {
  //         case s if s.is(_.Timeout) => chess.Status.Resign
  //         case s if s.is(_.NoStart) => chess.Status.Resign
  //         case s                    => game.status
  //       }).id.some,
  //       turns -> math.ceil(game.turns.toFloat / 2).some,
  //       rated -> game.rated.some,
  //       variant -> game.variant.id.some,
  //       uids -> game.userIds.toArray.some.filterNot(_.isEmpty),
  //       winner -> (game.winner flatMap (_.userId)),
  //       winnerColor -> game.winner.fold(3)(_.color.fold(1, 2)).some,
  //       averageRating -> game.averageUsersRating,
  //       ai -> game.aiLevel,
  //       date -> (ElasticSearch.Date.formatter print game.createdAt).some,
  //       duration -> game.estimateTotalTime.some,
  //       opening -> (game.opening map (_.code.toLowerCase)),
  //       analysed -> hasAnalyse.some,
  //       whiteUser -> game.whitePlayer.userId,
  //       blackUser -> game.blackPlayer.userId
  //     ).collect {
  //         case (key, Some(value)) => key -> value
  //       }: _*
  //   } id game.id
  // }
}
