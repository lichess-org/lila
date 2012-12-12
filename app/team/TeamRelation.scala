package lila
package team

case class TeamRelation(
    mine: Boolean,
    myRequest: Option[Request],
    requests: List[RequestWithUser]) {

  def isRequestedByMe = myRequest.nonEmpty

  def hasRequests = requests.nonEmpty
}
