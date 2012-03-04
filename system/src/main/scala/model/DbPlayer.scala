package lila.system
package model

case class DbPlayer(
    id: String,
    color: String,
    ps: String,
    aiLevel: Option[Int],
    isWinner: Option[Boolean],
    evts: String = "",
    elo: Option[Int]) {

  def eventStack = EventStack decode evts

  def isAi = aiLevel.isDefined
}
