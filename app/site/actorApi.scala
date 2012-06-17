package lila
package site

import socket.SocketMember

import scala.collection.mutable

case class Member(
    channel: JsChannel,
    username: Option[String]) extends SocketMember {

  val liveGames = mutable.Set[String]()

  def addLiveGames(ids: List[String]) {
    ids foreach liveGames.+=
  }
}

case class Join(
  uid: String,
  username: Option[String])
case class Connected(
  enumerator: JsEnumerator,
  channel: JsChannel)
