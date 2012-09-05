package lila
package search

import game.GameRepo
import core.Settings

import com.traackr.scalastic.elasticsearch.{ Indexer ⇒ EsIndexer }
import scalaz.effects._
import akka.dispatch.Future

final class SearchEnv(
    settings: Settings,
    gameRepo: GameRepo) {

  import settings._

  lazy val forms = new DataForm

  lazy val indexer = new Indexer(
    es = esIndexer,
    gameRepo = gameRepo)

  lazy val paginator = new PaginatorBuilder(
    indexer = indexer,
    maxPerPage = SearchPaginatorMaxPerPage)

  private lazy val esIndexer = EsIndexer.transport(
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
