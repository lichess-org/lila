package lila.forum

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
    lastPostIdTroll: String) {

  def id = _id

  def nbTopics(troll: Boolean): Int = troll.fold(nbTopicsTroll, nbTopics)
  def nbPosts(troll: Boolean): Int = troll.fold(nbPostsTroll, nbPosts)
  def lastPostId(troll: Boolean): String = troll.fold(lastPostIdTroll, lastPostId)

  def isStaff = slug == "staff"

  def isTeam = team.nonEmpty

  def withTopic(post: Post): Categ = copy(
    nbTopics = post.troll.fold(nbTopics, nbTopics + 1),
    nbPosts = post.troll.fold(nbPosts, nbPosts + 1),
    lastPostId = post.troll.fold(lastPostId, post.id),
    nbTopicsTroll = nbTopicsTroll + 1,
    nbPostsTroll = nbPostsTroll + 1,
    lastPostIdTroll = post.id)

  def slug = id
}
