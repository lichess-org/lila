package lila
package search

import game.{ GameRepo, DbGame }
import core.Settings

import com.traackr.scalastic.elasticsearch
import com.mongodb.casbah.MongoCollection

final class SearchEnv(
    settings: Settings,
    mongodb: String ⇒ MongoCollection,
    gameRepo: GameRepo) {

  import settings._

  lazy val forms = new DataForm

  lazy val indexer = new Indexer(
    es = esIndexer,
    gameRepo = gameRepo,
    queue = queue)

  lazy val paginator = new PaginatorBuilder(
    indexer = indexer,
    maxPerPage = SearchPaginatorMaxPerPage)

  def indexGame(game: DbGame) = queue enqueue game

  private lazy val queue = new Queue(
    collection = mongodb(SearchCollectionQueue))

  private lazy val esIndexer = elasticsearch.Indexer.transport(
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
