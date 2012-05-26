package lila
package forum

import org.joda.time.DateTime
import com.novus.salat.annotations.Key
import com.mongodb.DBRef
import ornicar.scalalib.OrnicarRandom

case class Post(
    @Key("_id") id: String,
    topicId: String,
    author: Option[String],
    user: Option[DBRef],
    text: String,
    number: Int,
    createdAt: DateTime) {

  def userId: Option[String] = user map (_.getId.toString)
}

object Post {

  val idSize = 8

  def apply(
    topicId: String,
    author: Option[String],
    user: Option[DBRef],
    text: String,
    number: Int): Post = Post(
    id = OrnicarRandom nextAsciiString idSize,
    topicId = topicId,
    author = author,
    user = user,
    text = text,
    number = number,
    createdAt = DateTime.now)
}
