package lila.mod

import lila.common.Chronometer
import lila.user.User
import lila.game.{Game, GameRepo, Pov}
import lila.analyse.{Analysis, AnalysisRepo, Accuracy}
import chess.Color

import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.Play.current

private final class NeuralApi(
    endpoint: String,
    assessApi: AssessApi) {

  import NeuralApi._

  def apply(user: User): Fu[Option[Result]] = {
    val games: Fu[List[Game]] = GameRepo.gamesForAssessment(user.id, 100)
    val gameIds: Fu[List[String]] = games flatMap { gs => gs flatMap (g => g.id)}
    val analysis: List[Fu[Option[Analysis]]] = games flatMap {
      gs => gs map {
        g => {
          val i: Fu[Option[Analysis]] = AnalysisRepo.doneById(g.id)
          i
        }
      }
    }
    ???
  }
  /*
    GameRepo.gamesForAssessment(user.id, 100) flatMap { gs =>
      (gs map { g => 
        AnalysisRepo.doneById(g.id) flatMap {
          case Some(a) => Chronometer.result {
            callEndpoint(toJson(g, a, user))
          }
          case _       => fuccess(none)
        }
      })
      ???
    }
    

  def apply(user: User): Fu[Option[Result]] =
    assessApi.getPlayerAssessmentsByUserId(user.id, 200) flatMap {
      case pas if pas.size < 1 => fuccess(none)
      case pas => Chronometer.result {
        callEndpoint(toJson(pas))
      } map (_.tuple) map (Result.apply _).tupled map some
    } recover {
      case e: Exception =>
        play.api.Logger("neural").warn(e.toString)
        none
    }
  */

  private def toJson(game: Game, analysis: Analysis, user: User) = JsArray {
    val color = game.player(user).fold(Color.white)(_.color)
    val player = game.player(color)
    val pov = Pov(game, color)
    Json.obj(
      // Main Image
      "moveTimes" -> game.moveTimes(color),
      "evals" -> Accuracy.evalsList(pov, analysis),
      "diffs" -> Accuracy.diffsList(pov, analysis),
      // Modifiers
      //"cpAvg" -> Accuracy.mean(pov, analysis),
      "blurs" -> game.player(color).blurs.toDouble / game.playerMoves(color),
      "holdAlert" -> player.holdAlert map {
        case Some(_) => true
        case _       => false
      },
      "isWinner" -> player.isWinner,
      "rating" -> player.rating,
      "isProvisional" -> player.provisional
    )
  }

  private implicit val answerReads = Json.reads[Answer]

  private def callEndpoint(input: JsValue): Fu[Answer] = WS.url(endpoint).post(Map(
    "input" -> Seq(Json stringify input)
  )) flatMap {
    case res if res.status == 200 => res.json.validate[Answer].fold(
      err => fufail(s"[neural] Can't parse answer ${res.body} $err"),
      fuccess _)
    case res => fufail(s"[neural] ${res.status} ${res.body}")
  }

}

object NeuralApi {

  case class Answer(decision: String, cheatPercent: Int)
  case class Result(answer: Answer, millis: Int)
}
