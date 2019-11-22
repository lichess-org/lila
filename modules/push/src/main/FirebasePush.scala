package lila.push

import java.io.FileInputStream
import collection.JavaConverters._
import scala.concurrent.Future

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.Play.current

private final class FirebasePush(
    getDevices: String => Fu[List[Device]],
    url: String
) {

  def apply(userId: String)(data: => PushApi.Data): Funit =
    getDevices(userId) flatMap {
      case Nil => funit
      case devices => getAccessToken flatMap { token =>
        // TODO batch send?
        // cf: https://firebase.google.com/docs/cloud-messaging/send-message#send_messages_to_multiple_devices
        Future.sequence(devices.map(send(token, _, data))).map(_ => ())
      }
    }

  private def send(authToken: String, device: Device, data: => PushApi.Data): Funit =
    WS.url(url)
      .withHeaders(
        "Authorization" -> s"Bearer $authToken",
        "Accept" -> "application/json",
        "Content-type" -> "application/json; UTF-8"
      )
      .post(Json.obj(
        "message" -> Json.obj(
          "token" -> device._id,
          "notification" -> Json.obj(
            "body" -> data.body,
            "data" -> data.payload,
            "title" -> data.title
          )
        )
      )) flatMap {
        case res if res.status == 200 => funit
        case res => fufail(s"[push] firebase: ${res.status} ${res.body}")
      }

  // TODO timeout conf
  private def getAccessToken(): Fu[String] = BlockingIO {
    val googleCredential = GoogleCredential
      .fromStream(getClass.getResourceAsStream("firebase-service-account.json"))
      .createScoped(Set("https://www.googleapis.com/auth/firebase.messaging").asJava)
    googleCredential.refreshToken()
    googleCredential.getAccessToken()
  }
}
