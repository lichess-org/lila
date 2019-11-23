package lila.push

import java.io.FileInputStream
import collection.JavaConverters._
import scala.concurrent.Future

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.Play
import Play.current

private final class FirebasePush(
    getDevices: String => Fu[List[Device]],
    url: String
) {

  def apply(userId: String)(data: => PushApi.Data): Funit =
    getDevices(userId) flatMap {
      case Nil => funit
      case devices => getAccessToken flatMap { token =>
        // TODO batch send
        // cf: https://firebase.google.com/docs/cloud-messaging/send-message#send_messages_to_multiple_devices
        Future.sequence(devices.map(send(token, _, data))).map(_ => ())
      }
    }

  private def send(authToken: String, device: Device, data: => PushApi.Data): Funit = {
    val message = Json.obj(
      "message" -> Json.obj(
        "token" -> device._id,
        // firebase doesn't support nested data object
        // and I only use what is inside userData
        "data" -> (data.payload \ "userData").get,
        "notification" -> Json.obj(
          "body" -> data.body,
          "title" -> data.title
        )
      )
    )
    println(Json.stringify(message))
    WS.url(url)
      .withHeaders(
        "Authorization" -> s"Bearer $authToken",
        "Accept" -> "application/json",
        "Content-type" -> "application/json; UTF-8"
      )
      .post(Json.obj(
        "message" -> Json.obj(
          "token" -> device._id,
          // firebase doesn't support nested data object and we only use what is
          // inside userData
          // note: send will silently fail if data contains any non string value
          "data" -> (data.payload \ "userData").get,
          "notification" -> Json.obj(
            "body" -> data.body,
            "title" -> data.title
          )
        )
      )) flatMap {
        case res if res.status == 200 => funit
        case res => fufail(s"[push] firebase: ${res.status} ${res.body}")
      }
  }

  // TODO timeout conf
  private def getAccessToken(): Fu[String] =
    Play.resourceAsStream("firebase-service-account.json").fold[Fu[String]] {
      fufail(s"[push] firebase: could not load firebase-service-account.json")
    } { file =>
      BlockingIO {
        val googleCredential = GoogleCredential
          .fromStream(file)
          .createScoped(Set("https://www.googleapis.com/auth/firebase.messaging").asJava)
        googleCredential.refreshToken()
        googleCredential.getAccessToken()
      }
    }
}
