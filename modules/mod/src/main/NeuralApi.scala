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
      } map (Result.apply _).tupled map some
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

  private def callEndpoint(input: JsValue): Fu[Decision] = {
    WS.url(endpoint).post(Map(
      "input" -> Seq(Json stringify input)
    )) flatMap {
      case res if res.status == 200 => res.body.lines.next match {
        case "NO-ACTION" => fuccess(Decision.NoAction)
        case "REPORT"    => fuccess(Decision.Report)
        case "MARK"      => fuccess(Decision.Mark)
        case err         => fufail(s"[neural] invalid response <$err>")
      }
      case res => fufail(s"[neural] ${res.status} ${res.body}")
    }
  }
}

object NeuralApi {

  case class Result(decision: Decision, millis: Int)

  sealed trait Decision
  object Decision {
    case object NoAction extends Decision
    case object Report extends Decision
    case object Mark extends Decision
  }
}
