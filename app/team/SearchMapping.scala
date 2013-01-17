package lila
package team

import search.ElasticSearch._

private[team] object SearchMapping {

  object fields {
    val name = "na"
    val description = "de"
    val location = "lo"
    val nbMembers = "nbm"
    val date = "da"
  }
  import fields._

  def mapping = {
    import Mapping._
    Map(
      "properties" -> List(
        field(name, "string"),
        field(description, "string"),
        field(location, "string"),
        field(nbMembers, "short"),
        field(date, "date", attrs = Map("format" -> Date.format))
      ).toMap
    )
  }

  def apply(team: Team): Pair[String, Map[String, Any]] = team.id -> (List(
    name -> team.name.some,
    description -> team.description.some,
    location -> team.location,
    nbMembers -> team.nbMembers.some,
    date -> (Date.formatter print team.createdAt).some
  ) collect {
      case (x, Some(y)) ⇒ x -> y
    }).toMap
}
