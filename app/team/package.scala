package lila

import scalaz.Zero

package object team extends TeamImplicits

trait TeamImplicits {

  import team.TeamRelation

  implicit def TeamRelationZero: Zero[TeamRelation] = new Zero[TeamRelation] {
    val zero = TeamRelation(mine = false, request = none)
  }

}
