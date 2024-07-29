import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal

// Case class to parse the tablebase result
case class TablebaseResult(
  dtz: Int,  // Distance to zeroing (moves to convert to a simpler endgame)
  wdl: Int,  // Win/draw/loss indicator
  bestMove: String
)

object TablebaseService {
  def getTablebaseEvaluation(fen: String): Future[TablebaseResult] = {
    val url = s"https://api.chessdb.cn:81/tablebase?fen=$fen"
    val response = Http().singleRequest(HttpRequest(uri = url))

    response.flatMap {
      case HttpResponse(StatusCodes.OK, _, entity, _) =>
        Unmarshal(entity).to[TablebaseResult]
      case _ =>
        Future.failed(new Exception("Failed to fetch tablebase data"))
    }
  }
}
