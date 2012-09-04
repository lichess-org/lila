package lila
package search

import play.api.data._
import play.api.data.Forms._
import scalaz.effects._

final class DataForm {

  val search = Form(mapping(
    "usernames" -> optional(nonEmptyText),
    "variant" -> optional(numberIn(Query.variants)),
    "mode" -> optional(numberIn(Query.modes)),
    "opening" -> optional(stringIn(Query.openings)),
    "turnsMin" -> optional(numberIn(Query.turns)),
    "turnsMax" -> optional(numberIn(Query.turns))
  )(SearchData.apply)(SearchData.unapply))

  private def numberIn(choices: List[(Int, String)]) =
    number.verifying(hasKey(choices, _))

  private def stringIn(choices: List[(String, String)]) =
    nonEmptyText.verifying(hasKey(choices, _))

  private def hasKey[A](choices: List[(A, _)], key: A) = 
    choices map (_._1) contains key
}

case class SearchData(
  usernames: Option[String] = None,
  variant: Option[Int],
  mode: Option[Int],
  opening: Option[String],
  turnsMin: Option[Int],
  turnsMax: Option[Int]
)
