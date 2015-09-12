package lila.security

import org.joda.time.DateTime

import play.api.libs.json._

final class Whois(key: String, api: Api, tor: Tor, userJson: lila.user.JsonView) {

  def apply(ip: String, reqKey: String): Fu[Either[String, JsObject]] =
    if (reqKey != key) fuccess(Left("Invalid key"))
    else api.userIdsSharingIp(ip) flatMap lila.user.UserRepo.byIds map { users =>
      Right(Json.obj(
        "ip" -> ip,
        "tor" -> tor.isExitNode(ip),
        "users" -> users.map { u =>
          userJson(u, true) ++ Json.obj(
            "nbGames" -> u.count.game,
            "closed" -> !u.enabled,
            "troll" -> u.troll,
            "ipBan" -> u.ipBan)
        }))
    }
}
