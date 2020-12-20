package lila.forum

import lila.user.User

case class Categ(
    _id: String, // slug
    name: String,
    desc: String,
    pos: Int,
    team: Option[String] = None,
    nbTopics: Int,
    nbPosts: Int,
    lastPostId: String,
    nbTopicsTroll: Int,
    nbPostsTroll: Int,
    lastPostIdTroll: String,
    quiet: Boolean = false
) {

  def id = _id

  def nbTopics(forUser: Option[User]): Int = if (forUser.exists(_.marks.troll)) nbTopicsTroll else nbTopics
  def nbPosts(forUser: Option[User]): Int  = if (forUser.exists(_.marks.troll)) nbPostsTroll else nbPosts
  def lastPostId(forUser: Option[User]): String =
    if (forUser.exists(_.marks.troll)) lastPostIdTroll else lastPostId

  def isTeam = team.nonEmpty

  def withPost(topic: Topic, post: Post): Categ =
    copy(
      nbTopics = if (post.troll) nbTopics else nbTopics + 1,
      nbPosts = if (post.troll) nbPosts else nbPosts + 1,
      lastPostId = if (post.troll || topic.isTooBig) lastPostId else post.id,
      nbTopicsTroll = nbTopicsTroll + 1,
      nbPostsTroll = nbPostsTroll + 1,
      lastPostIdTroll = if (topic.isTooBig) lastPostIdTroll else post.id
    )

  def slug = id
}

object Categ {

  private val TeamSlugPattern = """team-([\w-]++)""".r

  def slugToTeamId(slug: String) =
    slug match {
      case TeamSlugPattern(teamId) => teamId.some
      case _                       => none
    }
}
