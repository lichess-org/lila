package lila.mod

import lila.common.Chronometer
import lila.evaluation.PlayerAssessment
import lila.user.User
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.Play.current

private final class NeuralApi(
    endpoint: String,
    assessApi: AssessApi) {

  import NeuralApi._

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

  private def toJson(pas: List[PlayerAssessment]) = JsArray {
    def bool2int(b: Boolean) = b.fold(1, 0)
    pas map { pa =>
      Json.obj(
        "ser" -> pa.flags.suspiciousErrorRate.|>(bool2int),
        "aha" -> pa.flags.alwaysHasAdvantage.|>(bool2int),
        "cmt" -> pa.flags.consistentMoveTimes.|>(bool2int),
        "nfm" -> pa.flags.noFastMoves.|>(bool2int),
        "sha" -> pa.flags.suspiciousHoldAlert.|>(bool2int),
        "sfAvg" -> pa.sfAvg,
        "sfSd" -> pa.sfSd,
        "mtAvg" -> pa.mtAvg,
        "mtSd" -> pa.mtSd,
        "blurs" -> pa.blurs
      )
    }
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

  case class Answer(decision: String, cheatPercent: Int, nonCheatPercent: Int)
  case class Result(answer: Answer, millis: Int)
}
