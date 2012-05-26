package lila
package forum

import org.joda.time.DateTime
import com.novus.salat.annotations.Key
import com.mongodb.DBRef

case class Post(
    @Key("_id") id: String,
    topicId: String,
    author: String,
    user: Option[DBRef],
    text: String,
    number: Int,
    createdAt: DateTime) {

  def userId: Option[String] = user map (_.getId.toString)
}
