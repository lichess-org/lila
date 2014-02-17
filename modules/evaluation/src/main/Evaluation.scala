package lila.evaluation

import org.joda.time.DateTime
import lila.user.{ Perfs }

case class Evaluation(
    id: String,
    shallow: Evaluation.Percent,
    deep: Option[Evaluation.Percent],
    moveTime: Evaluation.Percent,
    blur: Evaluation.Percent,
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
    if (mark(perfs)) "definitely cheating"
    else if (report(perfs)) "suspicious"
    else "clean"

  def reportText(maxGames: Int = 10) = {
    val gameText = games take maxGames map { g => s"${g.url} $g" } mkString "\n"
    s"[AUTOREPORT] Cheat evaluation: $percent%\n\n$gameText"
  }

  def report(perfs: Perfs) = action == Report || {
    action == Mark && !mark(perfs)
  }

  def mark(perfs: Perfs) =
    action == Mark && deviationIsLow(perfs) && !ratingIsGreat(perfs)
}

object Evaluation {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  private[evaluation] val greatRatingThreshold = 2100
  private[evaluation] val ratingThreshold = 1700
  private[evaluation] val deviationThreshold = 120

  def deviationIsLow(perfs: Perfs) = perfs.global.glicko.deviation < deviationThreshold
  def ratingIsHigh(perfs: Perfs) = perfs.global.glicko.rating >= ratingThreshold
  def ratingIsGreat(perfs: Perfs) = perfs.global.glicko.rating >= greatRatingThreshold

  private type Percent = Int

  sealed trait Action
  case object Nothing extends Action
  case object Report extends Action
  case object Mark extends Action

  case class Game(
      url: String,
      moveTime: Option[Int],
      blur: Option[Int],
      error: Option[Int]) {

    override def toString = List(
      moveTime map (x => s"Move time deviation: $x%"),
      blur map (x => s"Blur rate: $x%"),
      error map (x => s"Error rate: $x%")
    ).flatten mkString ", "
  }

  private[evaluation] implicit val gameReader = Json.reads[Game]

  private[evaluation] val reader: Reads[Evaluation] = (
    (__ \ '_id).read[String] and
    (__ \ 'shallow).read[Int] and
    (__ \ 'deep).readNullable[Int] and
    (__ \ 'moveTime).read[Int] and
    (__ \ 'blur).read[Int] and
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
