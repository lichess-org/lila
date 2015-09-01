package lila.gameSearch

import akka.actor._
import com.typesafe.config.Config

import lila.db.api.$find
import lila.game.tube.gameTube
import lila.search._

final class Env(
    config: Config,
    system: ActorSystem,
    makeClient: Index => ESClient) {

  private val IndexName = config getString "index"
  private val TypeName = config getString "type"
  private val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
  private val IndexerName = config getString "indexer.name"
  private val ActorName = config getString "actor.name"

  private val client = makeClient(Index(IndexName))

  // private val indexer: ActorRef = system.actorOf(Props(new Indexer(
  //   client = client,
  //   indexName = IndexName,
  //   typeName = TypeName
  // )), name = IndexerName)

  private def converter(ids: Seq[String]) =
    $find.byOrderedIds[lila.game.Game](ids)

  lazy val paginator = new PaginatorBuilder[lila.game.Game, Query](
    searchApi = ???,
    maxPerPage = PaginatorMaxPerPage)

  lazy val forms = new DataForm

  system.actorOf(Props(new Actor {
    import lila.game.actorApi.{ InsertGame, FinishGame }
    context.system.lilaBus.subscribe(self, 'finishGame)
    def receive = {
      case FinishGame(game, _, _) => // self ! InsertGame(game)
      // case lila.analyse.actorApi.AnalysisReady(game, analysis) =>
      //   assessApi.onAnalysisReady(game, analysis)
      // case lila.game.actorApi.FinishGame(game, whiteUserOption, blackUserOption) =>
      //   (whiteUserOption |@| blackUserOption) apply {
      //     case (whiteUser, blackUser) => boosting.check(game, whiteUser, blackUser) >>
      //       assessApi.onGameReady(game, whiteUser, blackUser)
      //   }
    }
  }), name = ActorName)

  def cli = new lila.common.Cli {
    import akka.pattern.ask
    private implicit def timeout = makeTimeout minutes 60
    def process = {
      case "game" :: "search" :: "reset" :: Nil => fuccess("done")
    }
  }
}

object Env {

  lazy val current = "gameSearch" boot new Env(
    config = lila.common.PlayApp loadConfig "gameSearch",
    system = lila.common.PlayApp.system,
    makeClient = lila.search.Env.current.makeClient)
}
