package lila.push

import lila.user.User

import play.api.libs.json._
import play.api.libs.ws.{ WS, WSAuthScheme }
import play.api.Play.current

// https://aerogear.org/docs/specs/aerogear-unifiedpush-rest/index.html#397083935
private final class Aerogear(config: Aerogear.Config) {

  import Aerogear._

  def push(p: Push): Funit = WS.url(s"${config.url}/rest/sender")
    .withAuth(config.applicationId, config.masterSecret, WSAuthScheme.BASIC)
    .post(Json.obj(
      "message" -> p
    )).flatMap {
      case res if res.status == 200 => funit
      case res                      => fufail(s"[push] $p ${res.status} ${res.body}")
    }
}

private object Aerogear {

  case class Push(
    userId: String,
    alert: String,
    sound: String, // ???
    userData: JsObject)

  private implicit val pushWrites: OWrites[Push] = OWrites { p =>
    Json.obj(
      "alert" -> p.alert,
      "sound" -> p.sound,
      "criteria" -> Json.obj(
        "alias" -> List(p.userId),
        "categories" -> List("move")),
      "user-data" -> p.userData
    )
  }

  case class Config(
    url: String,
    variantId: String,
    secret: String,
    applicationId: String,
    masterSecret: String)
}
