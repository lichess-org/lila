package lila.message

import org.joda.time.DateTime
import ornicar.scalalib.Random

case class Post(
    id: String,
    text: String,
    isByCreator: Boolean,
    isRead: Boolean,
    createdAt: DateTime) {

  def isByInvited = !isByCreator

  def isUnRead = !isRead
}

object Posts {

  val idSize = 8

  def make(
    text: String,
    isByCreator: Boolean): Post = Post(
    id = Random nextString idSize,
    text = text,
    isByCreator = isByCreator,
    isRead = false,
    createdAt = DateTime.now)
}
