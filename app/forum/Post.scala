package lila
package forum

import org.joda.time.DateTime
import com.novus.salat.annotations.Key
import com.mongodb.casbah.Imports.ObjectId
import ornicar.scalalib.OrnicarRandom

import user.User

case class Post(
    @Key("_id") id: String,
    topicId: String,
    author: Option[String],
    userId: Option[ObjectId],
    text: String,
    number: Int,
    createdAt: DateTime) {

  def showAuthor = (author map (_.trim) filter ("" !=)) | User.anonymous

  def userIdString: Option[String] = userId map (_.toString)
}

object Post {

  val idSize = 8

  def apply(
    topicId: String,
    author: Option[String],
    userId: Option[ObjectId],
    text: String,
    number: Int): Post = Post(
    id = OrnicarRandom nextAsciiString idSize,
    topicId = topicId,
    author = author,
    userId = userId,
    text = text,
    number = number,
    createdAt = DateTime.now)
}
