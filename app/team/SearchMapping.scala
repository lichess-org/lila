package lila
package team

import search.ElasticSearch._

private[team] object SearchMapping {

  object fields {
    val name = "na"
    val description = "de"
    val location = "lo"
    val nbMembers = "nbm"
  }
  import fields._
  import Mapping.field

  def mapping = Map(
    "properties" -> List(
      field(name, "string"),
      field(description, "string"),
      field(location, "string"),
      field(nbMembers, "short")
    ).toMap
  )

  def apply(team: Team): Pair[String, Map[String, Any]] = team.id -> Map(
    name -> team.name,
    description -> team.description,
    location -> ~team.location,
    nbMembers -> team.nbMembers
  )
}
