package lila.forum

import lila.user.User

case class ForumCateg(
    _id: ForumCategId, // slug
    name: String,
    desc: String,
    team: Option[TeamId] = None,
    nbTopics: Int,
    nbPosts: Int,
    lastPostId: ForumPostId,
    nbTopicsTroll: Int,
    nbPostsTroll: Int,
    lastPostIdTroll: ForumPostId,
    quiet: Boolean = false,
    hidden: Boolean = false
):

  inline def id = _id

  def nbTopics(forUser: Option[User]): Int = if (forUser.exists(_.marks.troll)) nbTopicsTroll else nbTopics
  def nbPosts(forUser: Option[User]): Int  = if (forUser.exists(_.marks.troll)) nbPostsTroll else nbPosts
  def lastPostId(forUser: Option[User]): ForumPostId =
    if (forUser.exists(_.marks.troll)) lastPostIdTroll else lastPostId

  def isTeam = team.nonEmpty

  def withPost(topic: ForumTopic, post: ForumPost): ForumCateg =
    copy(
      // the `Topic` object is created before adding the post, hence why nbPosts is compared to 0 and not to 1
      nbTopics = if (post.troll || topic.nbPosts > 0) nbTopics else nbTopics + 1,
      nbPosts = if (post.troll) nbPosts else nbPosts + 1,
      lastPostId = if (post.troll || topic.isTooBig) lastPostId else post.id,
      nbTopicsTroll = if (topic.nbPostsTroll == 0) nbTopicsTroll + 1 else nbTopicsTroll,
      nbPostsTroll = nbPostsTroll + 1,
      lastPostIdTroll = if (topic.isTooBig) lastPostIdTroll else post.id
    )

  def slug = id

object ForumCateg:

  val ublogId = ForumCategId("community-blog-discussions")

  def isTeamSlug(id: ForumCategId) = id.value.startsWith("team-")

  def toTeamId(id: ForumCategId): Option[TeamId] = isTeamSlug(id) option TeamId(id.value.drop(5))
  def fromTeamId(id: TeamId): ForumCategId       = ForumCategId(s"team-$id")
