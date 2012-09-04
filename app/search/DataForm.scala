package lila
package search

import play.api.data._
import play.api.data.Forms._
import scalaz.effects._

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
    "aiLevel" -> numberIn(Query.aiLevels),
    "durationMin" -> numberIn(Query.durations),
    "durationMax" -> numberIn(Query.durations),
    "dateMin" -> stringIn(Query.dates),
    "dateMax" -> stringIn(Query.dates),
    "status" -> numberIn(Query.statuses)
  )(SearchData.apply)(SearchData.unapply))

  private def numberIn(choices: Seq[(Int, String)]) =
    optional(number.verifying(hasKey(choices, _)))

  private def stringIn(choices: Seq[(String, String)]) =
    optional(text.verifying(hasKey(choices, _)))

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
  aiLevel: Option[Int],
  durationMin: Option[Int],
  durationMax: Option[Int],
  dateMin: Option[String],
  dateMax: Option[String],
  status: Option[Int]
)
