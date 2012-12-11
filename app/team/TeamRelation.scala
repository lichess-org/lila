package lila
package team

case class TeamRelation(
    mine: Boolean,
    request: Option[Request]) {

  def isRequested = request.nonEmpty
}
