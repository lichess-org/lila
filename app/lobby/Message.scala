package lila
package lobby

import com.novus.salat.annotations.Key
import org.bson.types.ObjectId
import org.joda.time.DateTime

case class Message(
    @Key("_id") id: ObjectId = new ObjectId,
    username: String,
    text: String) {

  def render = Map(
    "txt" -> text,
    "u" -> username)

  def createdAt = new DateTime(id.getTime)

  def isEmpty = text == ""
}
