package lila.team

import play.api.libs.json._

import lila.common.Json.{ jodaWrites, markdownFormat }
import lila.user.LightUserApi

final class JsonView(lightUserApi: LightUserApi, userJson: lila.user.JsonView) {

  implicit val teamWrites = OWrites[Team] { team =>
    Json
      .obj(
        "id"          -> team.id,
        "name"        -> team.name,
        "description" -> team.description,
        "open"        -> team.open,
        "leader"      -> lightUserApi.sync(team.createdBy), // for BC
        "leaders"     -> team.leaders.flatMap(lightUserApi.sync),
        "nbMembers"   -> team.nbMembers
      )
  }

  implicit private val requestWrites = OWrites[Request] { req =>
    Json
      .obj(
        "userId"  -> req.user,
        "teamId"  -> req.team,
        "message" -> req.message,
        "date"    -> req.date
      )
      .add("declined" -> req.declined)
  }

  implicit val requestWithUserWrites = OWrites[RequestWithUser] { case RequestWithUser(req, user) =>
    Json.obj(
      "request" -> req,
      "user"    -> userJson.full(user, withOnline = false, withRating = true)
    )
  }
}
