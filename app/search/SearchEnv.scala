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
    EsIndexer.local.start ~ { _ ⇒ println("Done") }
  }
}
