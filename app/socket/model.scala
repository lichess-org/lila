package lila
package socket

case object GetNbMembers
case class NbPlayers(nb: Int)
case object GetUsernames
case object Close
