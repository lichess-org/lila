package lila
package message

import org.joda.time.DateTime
import ornicar.scalalib.OrnicarRandom

case class Post(
    id: String,
    text: String,
    isByCreator: Boolean,
    isRead: Boolean,
    createdAt: DateTime) {

  def isByInvited = !isByCreator

  def isUnRead = !isRead
}

object Post {

  val idSize = 8

  def apply(
    text: String,
    isByCreator: Boolean): Post = Post(
    id = OrnicarRandom nextString idSize,
    text = text,
    isByCreator = isByCreator,
    isRead = false,
    createdAt = DateTime.now)
}
