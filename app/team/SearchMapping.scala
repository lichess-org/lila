package lila.app
package team

import search.ElasticSearch._
import play.api.libs.json._

private[team] object SearchMapping {

  object fields {
    val name = "na"
    val description = "de"
    val location = "lo"
    val nbMembers = "nbm"
  }
  import fields._
  import Mapping._

  def mapping = Json.obj(
    "properties" -> Json.toJson(List(
      boost(name, "string", 3),
      boost(description, "string"),
      boost(location, "string"),
      field(nbMembers, "short")
    ).toMap),
    "analyzer" -> Json.toJson("snowball")
  )

  def apply(team: Team): Pair[String, JsObject] = team.id -> Json.obj(
    name -> team.name,
    description -> team.description,
    location -> ~team.location,
    nbMembers -> team.nbMembers
  )
}
