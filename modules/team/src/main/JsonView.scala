package lila.team

import play.api.libs.json._

import lila.user.LightUserApi

final class JsonView(lightUserApi: LightUserApi) {

  implicit val teamWrites: OWrites[Team] = OWrites[Team] { team =>
    Json
      .obj(
        "id"          -> team.id,
        "name"        -> team.name,
        "description" -> team.description,
        "open"        -> team.open,
        "leader"      -> lightUserApi.sync(team.createdBy), // for BC
        "leaders"     -> team.leaders.flatMap(lightUserApi.sync),
        "nbMembers"   -> team.nbMembers,
      )
      .add("location" -> team.location)
  }
}
