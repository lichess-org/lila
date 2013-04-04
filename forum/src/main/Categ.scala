package lila.forum

case class Categ(
    slug: String,
    name: String,
    desc: String,
    pos: Int,
    team: Option[String] = None,
    nbTopics: Int = 0,
    nbPosts: Int = 0,
    lastPostId: String = "") {

  def isStaff = slug == "staff"

  def isTeam = team.nonEmpty

  def id = slug
}

object Categ {

  import lila.db.Tube
  import Tube.Helpers._
  import play.api.libs.json._

  private implicit def topicTube = Topic.tube

  private val defaults = Json.obj(
    "team" -> none[String],
    "nbTopics" -> 0,
    "nbPosts" -> 0,
    "lastPostId" -> "")

  lazy val tube = Tube(
    reader = (__.json update merge(defaults)) andThen Json.reads[Categ],
    writer = Json.writes[Categ]
  )
}
