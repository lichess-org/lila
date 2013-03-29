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
