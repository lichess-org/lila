package lila.forumSearch

import akka.actor._
import com.typesafe.config.Config
import org.elasticsearch.action.search.SearchResponse
import play.api.libs.json.JsObject
import scalastic.elasticsearch.{ Indexer => EsIndexer }

import lila.forum.{ PostApi, Post => PostModel, PostView }
import lila.search.TypeIndexer

final class Env(
    config: Config,
    postApi: PostApi,
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
    mapping = Post.jsonMapping,
    indexQuery = indexQuery _
  )), name = IndexerName + "-low-level")

  private val indexer: ActorRef = system.actorOf(Props(new Indexer(
    lowLevel = lowLevelIndexer,
    postApi = postApi
  )), name = IndexerName)

  def apply(text: String, page: Int, staff: Boolean, troll: Boolean) =
    paginatorBuilder(Query(text, staff, troll), page)

  def cli = new lila.common.Cli {
    import akka.pattern.ask
    import lila.search.actorApi.RebuildAll
    private implicit def timeout = makeTimeout minutes 20
    def process = {
      case "forum" :: "search" :: "reset" :: Nil =>
        (lowLevelIndexer ? RebuildAll) inject "Forum search index rebuilt"
    }
  }

  private lazy val paginatorBuilder = new lila.search.PaginatorBuilder(
    indexer = lowLevelIndexer,
    maxPerPage = PaginatorMaxPerPage,
    converter = responseToPosts _)

  private def responseToPosts(response: SearchResponse): Fu[List[PostView]] =
    postApi viewsFromIds (response.getHits.hits.toList map (_.id))

  private def indexQuery(sel: JsObject): Funit = {
    import play.api.libs.json._
    import play.api.libs.iteratee._
    import lila.db.api._
    import lila.forum.tube.postTube
    esIndexer map { es =>
      $enumerate.bulk[Option[PostModel]]($query[PostModel](sel), 1000) { postOptions =>
        (postApi liteViews postOptions.flatten) map { views =>
          es bulk {
            views map { view =>
              es.index_prepare(
                IndexName,
                TypeName,
                view.post.id,
                Json stringify Post.from(view)
              ).request
            }
          }
        }
      }
    }
  }
}

object Env {

  lazy val current = "[boot] forumSearch" describes new Env(
    config = lila.common.PlayApp loadConfig "forumSearch",
    postApi = lila.forum.Env.current.postApi,
    system = lila.common.PlayApp.system,
    esIndexer = lila.search.Env.current.esIndexer)
}
