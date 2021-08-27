package lila.team

import lila.user.LightUserApi

import play.api.libs.json._

final class JsonView(lightUserApi: LightUserApi) {

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
}
