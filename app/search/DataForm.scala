package lila
package search

import play.api.data._
import play.api.data.Forms._
import scalaz.effects._
import org.joda.time.DateTime
import org.scala_tools.time.Imports._

import chess.{ Mode }

final class DataForm {

  val search = Form(mapping(
    "usernames" -> optional(nonEmptyText),
    "variant" -> numberIn(Query.variants),
    "mode" -> numberIn(Query.modes),
    "opening" -> stringIn(Query.openings),
    "turnsMin" -> numberIn(Query.turns),
    "turnsMax" -> numberIn(Query.turns),
    "eloMin" -> numberIn(Query.averageElos),
    "eloMax" -> numberIn(Query.averageElos),
    "hasAi" -> numberIn(Query.hasAis),
    "aiLevelMin" -> numberIn(Query.aiLevels),
    "aiLevelMax" -> numberIn(Query.aiLevels),
    "durationMin" -> numberIn(Query.durations),
    "durationMax" -> numberIn(Query.durations),
    "dateMin" -> stringIn(Query.dates),
    "dateMax" -> stringIn(Query.dates),
    "status" -> numberIn(Query.statuses),
    "sortField" -> nonEmptyText.verifying(hasKey(Sorting.fields, _)),
    "sortOrder" -> nonEmptyText.verifying(hasKey(Sorting.orders, _))
  )(SearchData.apply)(SearchData.unapply))

  private def numberIn(choices: Seq[(Int, String)]) =
    optional(number.verifying(hasKey(choices, _)))

  private def stringIn(choices: Seq[(String, String)]) =
    optional(nonEmptyText.verifying(hasKey(choices, _)))

  private def hasKey[A](choices: Seq[(A, _)], key: A) =
    choices map (_._1) contains key
}

case class SearchData(
    usernames: Option[String],
    variant: Option[Int],
    mode: Option[Int],
    opening: Option[String],
    turnsMin: Option[Int],
    turnsMax: Option[Int],
    eloMin: Option[Int],
    eloMax: Option[Int],
    hasAi: Option[Int],
    aiLevelMin: Option[Int],
    aiLevelMax: Option[Int],
    durationMin: Option[Int],
    durationMax: Option[Int],
    dateMin: Option[String],
    dateMax: Option[String],
    status: Option[Int],
    sortField: String = Sorting.default.field,
    sortOrder: String = Sorting.default.order) {

  lazy val query = Query(
    usernames = (~usernames).split(" ").toList map clean filter (_.nonEmpty),
    variant = variant,
    rated = mode flatMap Mode.apply map (_.rated),
    opening = opening map clean,
    turns = Range(turnsMin, turnsMax),
    averageElo = Range(eloMin, eloMax),
    hasAi = hasAi map (_ == 1),
    aiLevel = Range(aiLevelMin, aiLevelMax),
    duration = Range(durationMin, durationMax),
    date = Range(dateMin flatMap toDate, dateMax flatMap toDate),
    status = status,
    sorting = Sorting(sortField, sortOrder)
  )

  private def clean(s: String) = s.trim.toLowerCase

  private val DateDelta = """^(\d+)(\w)$""".r
  private def toDate(delta: String): Option[DateTime] = delta match {
    case DateDelta(n, "d") ⇒ parseIntOption(n) map (DateTime.now - _.days)
    case DateDelta(n, "w") ⇒ parseIntOption(n) map (DateTime.now - _.weeks)
    case DateDelta(n, "m") ⇒ parseIntOption(n) map (DateTime.now - _.months)
    case DateDelta(n, "y") ⇒ parseIntOption(n) map (DateTime.now - _.years)
    case _                 ⇒ None
  }
}
