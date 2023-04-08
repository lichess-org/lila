package lila.team

import play.api.libs.json.*

import lila.common.Json.given
import lila.user.LightUserApi

final class JsonView(lightUserApi: LightUserApi, userJson: lila.user.JsonView):

  given teamWrites: OWrites[Team] = OWrites { team =>
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

  given OWrites[Request] = OWrites { req =>
    Json
      .obj(
        "userId"  -> req.user,
        "teamId"  -> req.team,
        "message" -> req.message,
        "date"    -> req.date
      )
      .add("declined" -> req.declined)
  }

  given requestWithUserWrites: OWrites[RequestWithUser] = OWrites { case RequestWithUser(req, user) =>
    Json.obj(
      "request" -> req,
      "user"    -> userJson.full(user, withRating = true, withProfile = false)
    )
  }
