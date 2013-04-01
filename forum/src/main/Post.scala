package lila.forum

import org.joda.time.DateTime
import ornicar.scalalib.Random

import lila.user.Users

case class Post(
    id: String,
    topicId: String,
    categId: String,
    author: Option[String],
    userId: Option[String],
    ip: Option[String],
    text: String,
    number: Int,
    createdAt: DateTime) {

  def showAuthor = (author map (_.trim) filter ("" !=)) | Users.anonymous

  def showUsernameOrAuthor = userId | showAuthor

  def isTeam = categId startsWith teamSlug("")

  def isStaff = categId == "staff"
}

object Posts {

  val idSize = 8

  def apply(
    topicId: String,
    categId: String,
    author: Option[String],
    userId: Option[String],
    ip: Option[String],
    text: String,
    number: Int): Post = Post(
    id = Random nextString idSize,
    topicId = topicId,
    author = author,
    userId = userId,
    ip = ip,
    text = text,
    number = number,
    createdAt = DateTime.now,
    categId = categId)
}
