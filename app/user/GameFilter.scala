package lila
package user

import http.Context

import scalaz.NonEmptyLists

sealed abstract class GameFilter(val name: String)

object GameFilter {

  case object All extends GameFilter("all")
  case object Me extends GameFilter("me")
}

case class GameFilterMenu(
    info: UserInfo,
    me: Option[User]) extends NonEmptyLists {

  import GameFilter._

  val all = nel(All, List(
    (info.user.some != me) option Me
  ).flatten)

  def list = all.list

  def apply(name: String) = (list find (_.name == name)) | default

  def default = all.head
}
