package lila.teamSearch

import lila.search.TypeIndexer
import lila.team.{ Team ⇒ TeamModel }
import lila.db.api.$find

import com.typesafe.config.Config
import akka.actor._
import play.api.libs.json.JsObject
import scalastic.elasticsearch.{ Indexer ⇒ EsIndexer }
import org.elasticsearch.action.search.SearchResponse

final class Env(
    config: Config,
    system: ActorSystem,
    esIndexer: EsIndexer) {

  private val IndexName = config getString "index"
  private val TypeName = config getString "type"
  private val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
  private val IndexerName = config getString "indexer.name"

  private implicit val teamTube = lila.team.teamTube

  val indexer: ActorRef = system.actorOf(Props(new Indexer(
    lowLevel = lowLevelIndexer
  )), name = IndexerName)

  // lazy val paginatorBuilder = new lila.search.PaginatorBuilder(
  //   indexer = lowLevelIndexer,
  //   maxPerPage = PaginatorMaxPerPage,
  //   converter = responseToTeams _)

  // def cli = new lila.common.Cli {
  //   import akka.pattern.ask
  //   import lila.search.actorApi.RebuildAll
  //   private implicit def timeout = makeTimeout minutes 20
  //   def process = {
  //     case "team" :: "search" :: "reset" :: Nil ⇒
  //       (lowLevelIndexer ? RebuildAll) inject "team search index rebuilt"
  //   }
  // }

  private val lowLevelIndexer: ActorRef = system.actorOf(Props(new TypeIndexer(
    es = esIndexer,
    indexName = IndexName,
    typeName = TypeName,
    mapping = Team.jsonMapping,
    indexQuery = indexQuery _
  )), name = IndexerName + "-low-level")

  private def responseToTeams(response: SearchResponse): Fu[List[Team]] =
    $find.byOrderedIds[TeamModel](response.hits.hits.toList map (_.id))

  private def indexQuery(sel: JsObject): Funit = {
    import play.api.libs.json._
    import play.api.libs.concurrent.Execution.Implicits._
    import play.api.libs.iteratee._
    import lila.db.api._
    val cursor = postApi cursor sel
    cursor.enumerateBulks(1000) run {
      Iteratee foreach { (postOptions: Iterator[Option[TeamModel]]) ⇒
        val views = (postApi liteViews postOptions.toList.flatten).await
        esIndexer bulk {
          views map { view ⇒
            esIndexer.index_prepare(
              IndexName,
              TypeName,
              view.post.id,
              Json stringify Team.from(view)
            ).request
          }
        }
      }
    }
  }
}

object Env {

  lazy val current = "[teamSearch] boot" describes new Env(
    config = lila.common.PlayApp loadConfig "teamSearch",
    postApi = lila.team.Env.current.postApi,
    system = lila.common.PlayApp.system,
    esIndexer = lila.search.Env.current.esIndexer)
}
