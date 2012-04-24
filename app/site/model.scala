package lila
package site

import socket.SocketMember

case class Member(
    channel: Channel,
    username: Option[String]) extends SocketMember

case class Join(
  uid: String,
  username: Option[String])
case class Connected(channel: Channel)
