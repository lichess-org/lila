package lila.teamSearch

private[teamSearch] object Fields {
  val name = "na"
  val description = "de"
  val location = "lo"
  val nbMembers = "nbm"
}
// import fields._
// import ElasticSearch.Mapping._

// def jsonMapping = Json.obj(
//   "properties" -> Json.toJson(List(
//     boost(name, "string", 3),
//     boost(description, "string"),
//     boost(location, "string"),
//     field(nbMembers, "short")
//   ).toMap),
//   "analyzer" -> "snowball"
// )

// def from(team: TeamModel): JsObject = Json.obj(
//   name -> team.name,
//   description -> team.description,
//   location -> ~team.location,
//   nbMembers -> team.nbMembers
// )
// }
