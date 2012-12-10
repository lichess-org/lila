package lila
package team

import org.joda.time.{ DateTime, Duration }
import org.scala_tools.time.Imports._

import user.User

case class Member(
    id: String,
    createdAt: DateTime) {

  def is(userId: String): Boolean = id == userId
  def is(user: User): Boolean = is(user.id)
}

object Member {

  def apply(user: User): Member = apply(user.id)

  def apply(user: String): Member = new Member(id = user, createdAt = DateTime.now)
}
