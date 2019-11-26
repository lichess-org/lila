package lila.push

import java.io.FileInputStream
import scala.concurrent.Future

import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.AccessToken
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.Play.current

private final class FirebasePush(
    credentialsOpt: Option[GoogleCredentials],
    getDevices: String => Fu[List[Device]],
    url: String
) {

  def apply(userId: String)(data: => PushApi.Data): Funit =
    credentialsOpt.fold(fuccess({})) { creds =>
      getDevices(userId) flatMap {
        case Nil => funit
        // access token has 1h lifetime and is requested only if expired
        case devices => BlockingIO {
          creds.refreshIfExpired()
          creds.getAccessToken()
        } flatMap { token =>
          // TODO http batch request is possible using a multipart/mixed content
          // unfortuntely it doesn't seem easily doable with play WS
          Future.sequence(devices.map(send(token, _, data))).map(_ => ())
        }
      }
    }

  private def send(token: AccessToken, device: Device, data: => PushApi.Data): Funit = {
    WS.url(url)
      .withHeaders(
        "Authorization" -> s"Bearer ${token.getTokenValue}",
        "Accept" -> "application/json",
        "Content-type" -> "application/json; UTF-8"
      )
      .post(Json.obj(
        "message" -> Json.obj(
          "token" -> device._id,
          // firebase doesn't support nested data object and we only use what is
          // inside userData
          // note: send will silently fail if data contains any non string value
          "data" -> (data.payload \ "userData").asOpt[JsObject].map(prefixKeys(_)),
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

  private def prefixKeys(obj: JsObject): JsObject =
    JsObject(obj.fields.map {
      case (k, v) => s"lichess.$k" -> v
    })
}
