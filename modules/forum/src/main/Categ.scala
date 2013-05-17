package lila.forum

case class Categ(
    id: String, // slug
    name: String,
    desc: String,
    pos: Int,
    team: Option[String] = None,
    nbTopics: Int,
    nbPosts: Int,
    lastPostId: String) {

  def isStaff = slug == "staff"

  def isTeam = team.nonEmpty

  def withTopic(post: Post): Categ = copy(
    nbTopics = nbTopics + 1,
    nbPosts = nbPosts + 1,
    lastPostId = post.id)

  def slug = id
}

object Categ {

  import lila.db.Tube
  import Tube.Helpers._
  import play.api.libs.json._

  private implicit def topicTube = Topic.tube

  private def defaults = Json.obj("team" -> none[String])

  private[forum] lazy val tube = Tube(
    reader = (__.json update merge(defaults)) andThen Json.reads[Categ],
    writer = Json.writes[Categ]
  )
}
