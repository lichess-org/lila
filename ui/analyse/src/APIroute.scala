import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route

object ApiRoutes {
  val route: Route =
    pathPrefix("api") {
      path("analyze" / "tablebase") {
        parameters('fen) { fen =>
          complete {
            TablebaseService.getTablebaseEvaluation(fen).map { result =>
              HttpEntity(ContentTypes.`application/json`, s"""{
                "bestMove": "${result.bestMove}",
                "evaluation": ${result.wdl},
                "dtz": ${result.dtz}
              }""")
            }
          }
        }
      } ~
      path("practice" / Segment) { theme =>
        get {
          val position = PracticeService.generatePracticePosition(theme)
          complete(HttpEntity(ContentTypes.`application/json`, s"""{"fen": "$position"}"""))
        }
      }
    }
}
