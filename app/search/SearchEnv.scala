package lila
package search

import game.{ GameRepo, PgnRepo, DbGame }
import core.Settings

import scalastic.elasticsearch
import com.mongodb.casbah.MongoCollection

final class SearchEnv(
    settings: Settings,
    mongodb: String ⇒ MongoCollection,
    gameRepo: GameRepo,
    pgnRepo: PgnRepo) {

  import settings._

  lazy val forms = new DataForm

  lazy val indexer = new GameIndexer(
    es = esIndexer,
    gameRepo = gameRepo,
    pgnRepo = pgnRepo,
    queue = queue)

  lazy val paginator = new PaginatorBuilder(
    indexer = indexer,
    maxPerPage = SearchPaginatorMaxPerPage)

  def indexGame(game: DbGame) = queue enqueue game

  private lazy val queue = new Queue(
    collection = mongodb(SearchCollectionQueue))

  lazy val esIndexer = elasticsearch.Indexer.transport(
    settings = Map(
      "cluster.name" -> SearchESCluster
    ),
    host = SearchESHost,
    ports = Seq(SearchESPort)
  ) ~ { transport ⇒
      println("Start ElasticSearch")
      transport.start
      println("ElasticSearch is running")
    }
}
