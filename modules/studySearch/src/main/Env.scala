package lila.studySearch

import akka.actor._
import com.typesafe.config.Config

import lila.db.dsl._
import lila.search._

final class Env(
    config: Config,
    studyEnv: lila.study.Env,
    makeClient: Index => ESClient,
    system: ActorSystem) {

  private val IndexName = config getString "index"
  private val MaxPerPage = config getInt "paginator.max_per_page"
  private val ActorName = config getString "actor.name"

  private val client = makeClient(Index(IndexName))

  val api = new StudySearchApi(client, studyEnv.studyRepo, studyEnv.chapterRepo)

  def apply(text: String, page: Int) = paginatorBuilder(Query(text), page)

  def cli = new lila.common.Cli {
    def process = {
      case "study" :: "search" :: "reset" :: Nil => api.reset inject "done"
    }
  }

  private lazy val paginatorBuilder =
    new lila.search.PaginatorBuilder[lila.study.Study, Query](
      searchApi = api,
      maxPerPage = MaxPerPage)

  // system.actorOf(Props(new Actor {
  //   import lila.study.actorApi._
  //   def receive = {
  //     case InsertTeam(team) => api store team
  //     case RemoveTeam(id)   => client deleteById Id(id)
  //   }
  // }), name = ActorName)
}

object Env {

  lazy val current = "studySearch" boot new Env(
    config = lila.common.PlayApp loadConfig "studySearch",
    studyEnv = lila.study.Env.current,
    makeClient = lila.search.Env.current.makeClient,
    system = lila.common.PlayApp.system)
}
