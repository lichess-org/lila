package lila.app
package forum

import org.joda.time.DateTime
import com.novus.salat.annotations.Key
import ornicar.scalalib.Random

import user.User

case class Post(
    @Key("_id") id: String,
    topicId: String,
    categId: String,
    author: Option[String],
    userId: Option[String],
    ip: Option[String],
    text: String,
    number: Int,
    createdAt: DateTime) {

  def showAuthor = (author map (_.trim) filter ("" !=)) | User.anonymous

  def showUsernameOrAuthor = userId | showAuthor

  def isTeam = categId startsWith "team-"

  def isStaff = categId == "staff"
}

object Post {

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
