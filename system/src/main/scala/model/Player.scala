package lila.system
package model

import com.novus.salat.annotations._
import com.mongodb.casbah.Imports._

case class Player(
    id: String,
    color: String,
    ps: String,
    aiLevel: Option[Int],
    isWinner: Option[Boolean],
    evts: Option[String],
    elo: Option[Int]) {

  def isAi = aiLevel.isDefined
}
