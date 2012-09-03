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

  lazy val indexer = new Indexer(
    es = esIndexer,
    gameRepo = gameRepo)

  private lazy val esIndexer = {
    println("Start ElasticSearch")

    //val i = EsIndexer.using(Map())
    //val i = EsIndexer.local
    val i = EsIndexer.transport(
      settings = Map(
        "cluster.name" -> "elasticsearch"
      ),
      host = "localhost",
      ports = Seq(9300))

    i.start ~ { _ ⇒ println("Done") }
  }
}
