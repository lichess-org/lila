package lila.app
package lobby

import com.novus.salat.annotations.Key
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.libs.json.{ Json, JsObject }

case class Message(
    @Key("_id") id: ObjectId = new ObjectId,
    username: String,
    text: String) {

  def render: JsObject = Json.obj("txt" -> text, "u" -> username)

  def createdAt = new DateTime(id.getTime)

  def isEmpty = text == ""
}
