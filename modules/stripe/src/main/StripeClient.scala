package lila.stripe

import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.Play.current

import lila.common.PimpedJson._
import lila.user.{ User, UserRepo }

private final class StripeClient(config: StripeClient.Config) {

  def createCustomer(user: User, source: Source, plan: Plan): Fu[CustomerId] =
    UserRepo email user.id flatMap { email =>
      WS.url(config url "customers").post(Json.obj(
        "source" -> source.value,
        "plan" -> plan.id,
        "email" -> email,
        "description" -> user.titleName,
        "metadata" -> Json.obj("id" -> user.id)
      )).flatMap {
        case res if res.status == 200 => fuccess(CustomerId((res.json \ "id").as[String]))
        case res                      => fufail(s"[stripe] createCustomer ${res.status} ${res.body}")
      }
    }

  def customerExists(id: CustomerId): Fu[Boolean] =
    WS.url(config url s"customers/$id").get() flatMap {
      case res if res.status == 200 => fuccess(true)
      case res if res.status == 404 => fuccess(false)
      case res                      => fufail(s"[stripe] customerExists ${res.status} ${res.body}")
    }
}

private object StripeClient {

  case class Config(endpoint: String, publicKey: String, privateKey: String) {

    def url(end: String) = s"$endpoint/$end"
  }
}
