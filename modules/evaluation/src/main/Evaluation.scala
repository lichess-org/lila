package lila.evaluation

import lila.rating.Perf
import lila.user.{ User, Perfs }
import org.joda.time.DateTime

case class Evaluation(
    id: String,
    shallow: Evaluation.Percent,
    deep: Option[Evaluation.Percent],
    moveTime: Evaluation.Percent,
    blur: Evaluation.Percent,
    hold: Evaluation.Percent,
    analysis: Evaluation.Percent,
    progress: Evaluation.Percent,
    sharedIP: Evaluation.Percent,
    action: Evaluation.Action,
    games: List[Evaluation.Game],
    date: DateTime) {

  import Evaluation._

  def percent = deep getOrElse shallow

  def isDeep = deep.isDefined

  def verdict(perfs: Perfs) =
    if (watchPerfs(perfs) exists mark) "definitely cheating"
    else if (watchPerfs(perfs) exists report) "suspicious"
    else "clean"

  def verdict(perf: Perf) =
    if (mark(perf)) "definitely cheating"
    else if (report(perf)) "suspicious"
    else "clean"

  def reportText(maxGames: Int = 10) = {
    val gameText = games take maxGames map { g => s"${g.url}\n$g" } mkString "\n"
    s"[AUTOREPORT] Cheat evaluation: $percent%\n\n$gameText"
  }

  def report(perf: Perf) = action == Report || {
    action == Mark && !mark(perf)
  }

  def mark(perf: Perf) =
    action == Mark && heuristics.deviationIsLow(perf) && !heuristics.escapesAutoMark(perf)

  def gameIdsToAnalyse: List[String] =
    games take 9 filterNot (_.analysed) take 5 flatMap (_.gameId)
}

object Evaluation {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

 private[evaluation] def watchPerfs(p: Perfs): List[Perf] =
    List(p.bullet, p.blitz, p.classical, p.chess960, p.antichess, p.atomic, p.kingOfTheHill, p.threeCheck)

  private[evaluation] object heuristics {

    def progressIsHigh(perf: Perf) = perf.progress > 60
    def progressIsVeryHigh(perf: Perf) = perf.progress > 100
    def deviationIsLow(perf: Perf) = perf.glicko.deviation < 190
    def ratingIsHigh(perf: Perf) = perf.glicko.rating >= 1600
    def ratingIsGreat(perf: Perf) = perf.glicko.rating >= 2300
    def hasManyGames(perf: Perf) = perf.nb >= 50
    def escapesAutoMark(perf: Perf) = ratingIsGreat(perf) && hasManyGames(perf)
  }

  private type Percent = Int

  sealed trait Action
  case object Nothing extends Action
  case object Report extends Action
  case object Mark extends Action

  private val GameIdRegex = """^.+(\w{8})(?:/black)?$""".r

  case class Game(
      url: String,
      moveTime: Option[Int],
      blur: Option[Int],
      error: Option[Int],
      hold: Option[Int]) {

    def analysed = ~error != 0

    def gameId = url match {
      case GameIdRegex(id) => id.some
      case _               => none
    }

    def path = url.replace("http://lichess.org", "")

    override def toString = List(
      moveTime map (x => s"Move time: $x%"),
      blur map (x => s"Blur: $x%"),
      hold.filter(0!=) map (x => s"Bot: $x%"),
      error map (x => s"Analysis: $x%"),
      hold map (x => s"Bot: $x%")
    ).flatten mkString ", "
  }

  private[evaluation] implicit val gameReader = Json.reads[Game]

  private[evaluation] val reader: Reads[Evaluation] = (
    (__ \ '_id).read[String] and
    (__ \ 'shallow).read[Int] and
    (__ \ 'deep).readNullable[Int] and
    (__ \ 'moveTime).read[Int] and
    (__ \ 'blur).read[Int] and
    (__ \ 'hold).readNullable[Int].map(~_) and
    (__ \ 'analysis).read[Int] and
    (__ \ 'progress).read[Int] and
    (__ \ 'sharedIP).read[Int] and
    ((__ \ 'action).read[String].map(_.toLowerCase).map {
      case "report" => Report
      case "mark"   => Mark
      case _        => Nothing
    }) and
    (__ \ 'games).read[List[Game]] and
    (__ \ 'date).read[DateTime]
  )(Evaluation.apply _)

}
