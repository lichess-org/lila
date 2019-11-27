package lila.push

import com.google.auth.oauth2.{ GoogleCredentials, AccessToken }
import java.util.concurrent.atomic.AtomicReference
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.Play.current
import scala.concurrent.Future

import lila.user.User

private final class FirebasePush(
    credentialsOpt: Option[GoogleCredentials],
    getDevices: User.ID => Fu[List[Device]],
    url: String
) {

  private object sequentialBlock {

    private val queue: AtomicReference[Option[Fu[AccessToken]]] = new AtomicReference(none)

    def apply(blockingCall: => AccessToken): Fu[AccessToken] =
      queue.updateAndGet((prev: Option[Fu[AccessToken]]) => some {
        prev match {
          case None => Future(blockingCall)
          case Some(previous) => previous >> Future(blockingCall)
        }
      }).get
  }

  def apply(userId: User.ID)(data: => PushApi.Data): Funit =
    credentialsOpt ?? { creds =>
      getDevices(userId) flatMap {
        case Nil => funit
        // access token has 1h lifetime and is requested only if expired
        case devices => sequentialBlock {
          creds.refreshIfExpired()
          creds.getAccessToken()
        } flatMap { token =>
          // TODO http batch request is possible using a multipart/mixed content
          // unfortuntely it doesn't seem easily doable with play WS
          devices.map(send(token, _, data)).sequenceFu.void
        }
      }
    }

  private def send(token: AccessToken, device: Device, data: => PushApi.Data): Funit =
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
          "data" -> (data.payload \ "userData").asOpt[JsObject].map(transform(_)),
          "notification" -> Json.obj(
            "body" -> data.body,
            "title" -> data.title
          )
        )
      )) flatMap {
        case res if res.status == 200 => funit
        case res => fufail(s"[push] firebase: ${res.status} ${res.body}")
      }

  // filter out any non string value, otherwise Firebase API silently rejects
  // the request
  private def transform(obj: JsObject): JsObject =
    JsObject(obj.fields.collect {
      case (k, v: JsString) => s"lichess.$k" -> v
      case (k, v: JsNumber) => s"lichess.$k" -> JsString(v.toString)
    })
}
