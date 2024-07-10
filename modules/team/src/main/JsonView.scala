package lila.team

import play.api.libs.json.*

import lila.common.Json.given
import lila.core.user.LightUserApi

final class JsonView(lightUserApi: LightUserApi, userJson: lila.core.user.JsonView):

  given teamWrites: OWrites[Team] = OWrites: team =>
    Json
      .obj(
        "id"          -> team.id,
        "name"        -> team.name,
        "description" -> team.description,
        "open"        -> team.open,
        "leader"      -> lightUserApi.sync(team.createdBy), // for BC
        "nbMembers"   -> team.nbMembers
      )
      .add("flair" -> team.flair)

  given memberWithPermsWrites: OWrites[TeamMember] = OWrites: m =>
    Json.obj("user" -> lightUserApi.syncFallback(m.user), "perms" -> m.perms.map(_.key))

  given teamWithLeadersWrites: OWrites[Team.WithPublicLeaderIds] = OWrites: t =>
    teamWrites.writes(t.team) ++ Json.obj("leaders" -> t.publicLeaders.map(lightUserApi.syncFallback))

  given OWrites[TeamRequest] = OWrites: req =>
    Json
      .obj(
        "userId"  -> req.user,
        "teamId"  -> req.team,
        "message" -> req.message,
        "date"    -> req.date
      )
      .add("declined" -> req.declined)

  given requestWithUserWrites: OWrites[RequestWithUser] = OWrites:
    case RequestWithUser(req, user) =>
      Json.obj(
        "request" -> req,
        "user"    -> userJson.full(user.user, user.perfs.some, withProfile = false)
      )
