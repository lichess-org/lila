package lila.system
package model

case class DbPlayer(
    id: String,
    color: String,
    ps: String,
    aiLevel: Option[Int],
    isWinner: Option[Boolean],
    evts: Option[String],
    elo: Option[Int]) {

  def isAi = aiLevel.isDefined
}
