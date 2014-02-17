package lila.teamSearch

import akka.actor._
import com.typesafe.config.Config
import org.elasticsearch.action.search.SearchResponse
import play.api.libs.json.JsObject
import scalastic.elasticsearch.{ Indexer => EsIndexer }

import lila.db.api.{ $find, $cursor }
import lila.search.TypeIndexer
import lila.team.tube.teamTube
import lila.team.{ Team => TeamModel }

final class Env(
    config: Config,
    system: ActorSystem,
    esIndexer: Fu[EsIndexer]) {

  private val IndexName = config getString "index"
  private val TypeName = config getString "type"
  private val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
  private val IndexerName = config getString "indexer.name"

  private val lowLevelIndexer: ActorRef = system.actorOf(Props(new TypeIndexer(
    esIndexer = esIndexer,
    indexName = IndexName,
    typeName = TypeName,
    mapping = Team.jsonMapping,
    indexQuery = indexQuery _
  )), name = IndexerName + "-low-level")

  private val indexer: ActorRef = system.actorOf(Props(new Indexer(
    lowLevel = lowLevelIndexer
  )), name = IndexerName)

  def apply(text: String, page: Int) = paginatorBuilder(Query(text), page)

  def cli = new lila.common.Cli {
    import akka.pattern.ask
    import lila.search.actorApi.RebuildAll
    private implicit def timeout = makeTimeout minutes 20
    def process = {
      case "team" :: "search" :: "reset" :: Nil =>
        (lowLevelIndexer ? RebuildAll) inject "team search index rebuilt"
    }
  }

  private lazy val paginatorBuilder = new lila.search.PaginatorBuilder(
    indexer = lowLevelIndexer,
    maxPerPage = PaginatorMaxPerPage,
    converter = responseToTeams _)

  private def responseToTeams(response: SearchResponse): Fu[List[TeamModel]] =
    $find.byOrderedIds[TeamModel](response.getHits.hits.toList map (_.id))

  private def indexQuery(sel: JsObject): Funit = {
    import play.api.libs.json._
    import play.api.libs.iteratee._
    import lila.db.api._
    esIndexer map { es =>
      $enumerate.bulk[Option[TeamModel]]($query[TeamModel](sel), 100) { teamOptions =>
        fuccess {
          es bulk {
            teamOptions.flatten map { team =>
              es.index_prepare(
                IndexName,
                TypeName,
                team.id,
                Json stringify Team.from(team)
              ).request
            }
          }
        }
      }
    }
  }
}

object Env {

  lazy val current = "[boot] teamSearch" describes new Env(
    config = lila.common.PlayApp loadConfig "teamSearch",
    system = lila.common.PlayApp.system,
    esIndexer = lila.search.Env.current.esIndexer)
}
