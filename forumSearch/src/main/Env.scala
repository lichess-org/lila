package lila.forumSearch

import lila.search.TypeIndexer
import lila.forum.{ PostApi, Post ⇒ PostModel, PostView }
import lila.db.TubeInColl

import com.typesafe.config.Config
import akka.actor._
import play.api.libs.json.JsObject
import scalastic.elasticsearch.{ Indexer ⇒ EsIndexer }
import org.elasticsearch.action.search.SearchResponse

final class Env(
    config: Config,
    postApi: PostApi,
    system: ActorSystem,
    esIndexer: EsIndexer) {

  private val IndexName = config getString "index"
  private val TypeName = config getString "type"
  private val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
  private val IndexerName = config getString "indexer.name"

  val indexer: ActorRef = system.actorOf(Props(new Indexer(
    lowLevel = lowLevelIndexer,
    postApi = postApi
  )), name = IndexerName)

  lazy val paginatorBuilder = new lila.search.PaginatorBuilder(
    indexer = lowLevelIndexer,
    maxPerPage = PaginatorMaxPerPage,
    converter = responseToPosts _)

  private val lowLevelIndexer: ActorRef = system.actorOf(Props(new TypeIndexer(
    es = esIndexer,
    indexName = IndexName,
    typeName = TypeName,
    mapping = Post.jsonMapping,
    indexQuery = indexQuery _
  )), name = IndexerName + "-low-level")

  private def responseToPosts(response: SearchResponse): Fu[List[PostView]] =
    postApi viewsFromIds (response.hits.hits.toList map (_.id))

  private def indexQuery(sel: JsObject): Funit = {
    import play.api.libs.json._
    import play.api.libs.concurrent.Execution.Implicits._
    import play.api.libs.iteratee._
    import lila.db.api._
    val cursor = postApi cursor sel
    cursor.enumerateBulks(1000) run {
      Iteratee foreach { (postOptions: Iterator[Option[PostModel]]) ⇒
        val views = (postApi liteViews postOptions.toList.flatten).await
        esIndexer bulk {
          views map { view ⇒
            esIndexer.index_prepare(
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

object Env {

  lazy val current = "[forumSearch] boot" describes new Env(
    config = lila.common.PlayApp loadConfig "forumSearch",
    postApi = lila.forum.Env.current.postApi,
    system = play.api.libs.concurrent.Akka.system(play.api.Play.current),
    esIndexer = lila.search.Env.current.esIndexer)
}
