package lila

import scalaz.Zero

package object team extends TeamImplicits

trait TeamImplicits {

  import team.TeamRelation

  implicit def TeamRelationZero = new Zero[team.TeamRelation] {
    val zero = team.TeamRelation(
      mine = false,
      myRequest = none,
      requests = Nil)
  }
}
