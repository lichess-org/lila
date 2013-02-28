package lila.app
package site

import socket.SocketMember

import play.api.libs.json._

case class Member(
  channel: JsChannel,
  username: Option[String],
  flag: Option[String]) extends SocketMember {

  def hasFlag(f: String) = f.some == flag
}

case class SendToFlag(
    flag: String,
    message: JsObject) 

case class Join(
  uid: String,
  username: Option[String],
  flag: Option[String])
case class Connected(
  enumerator: JsEnumerator,
  channel: JsChannel)
