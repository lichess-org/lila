package lila.forum

import scala.util.chaining.*
import ornicar.scalalib.ThreadLocalRandom

import lila.common.config.MaxPerPage
import lila.user.User

case class ForumTopic(
    _id: ForumTopicId,
    categId: ForumCategId,
    slug: String,
    name: String,
    createdAt: Instant,
    updatedAt: Instant,
    nbPosts: Int,
    lastPostId: ForumPostId,
    updatedAtTroll: Instant,
    nbPostsTroll: Int,
    lastPostIdTroll: ForumPostId,
    troll: Boolean,
    closed: Boolean,
    sticky: Option[Boolean],
    userId: Option[UserId] = None, // only since SB mutes
    ublogId: Option[String] = None
):

  inline def id = _id

  def updatedAt(forUser: Option[User]): Instant =
    if (forUser.exists(_.marks.troll)) updatedAtTroll else updatedAt
  def nbPosts(forUser: Option[User]): Int   = if (forUser.exists(_.marks.troll)) nbPostsTroll else nbPosts
  def nbReplies(forUser: Option[User]): Int = nbPosts(forUser) - 1
  def lastPostId(forUser: Option[User]): ForumPostId =
    if (forUser.exists(_.marks.troll)) lastPostIdTroll else lastPostId

  def open = !closed

  def isTooBig = nbPosts > (if (ForumCateg.isTeamSlug(categId)) 500 else 50)

  def possibleTeamId = ForumCateg toTeamId categId

  def isSticky = ~sticky

  def isAuthor(user: User): Boolean = userId contains user.id
  def isUblog                       = ublogId.isDefined
  def isUblogAuthor(user: User)     = isUblog && isAuthor(user)

  def withPost(post: ForumPost): ForumTopic =
    copy(
      nbPosts = if (post.troll) nbPosts else nbPosts + 1,
      lastPostId = if (post.troll) lastPostId else post.id,
      updatedAt = if (isTooBig || post.troll) updatedAt else post.createdAt,
      nbPostsTroll = nbPostsTroll + 1,
      lastPostIdTroll = post.id,
      updatedAtTroll = if (isTooBig) updatedAt else post.createdAt
    )

  def incNbPosts = copy(nbPosts = nbPosts + 1)

  def isOld = updatedAt isBefore nowInstant.minusMonths(1)

  def lastPage(maxPerPage: MaxPerPage): Int =
    (nbPosts + maxPerPage.value - 1) / maxPerPage.value

object ForumTopic:

  def nameToId(name: String) =
    (lila.common.String slugify name) pipe { slug =>
      // if most chars are not latin, go for random slug
      if (slug.lengthIs > (name.lengthIs / 2)) slug else ThreadLocalRandom nextString 8
    }

  val idSize = 8

  def make(
      categId: ForumCategId,
      slug: String,
      name: String,
      userId: UserId,
      troll: Boolean,
      ublogId: Option[String] = None
  ): ForumTopic = ForumTopic(
    _id = ForumTopicId(ThreadLocalRandom nextString idSize),
    categId = categId,
    slug = slug,
    name = name,
    createdAt = nowInstant,
    updatedAt = nowInstant,
    nbPosts = 0,
    lastPostId = ForumPostId(""),
    updatedAtTroll = nowInstant,
    nbPostsTroll = 0,
    lastPostIdTroll = ForumPostId(""),
    troll = troll,
    userId = userId.some,
    closed = false,
    sticky = None,
    ublogId = ublogId
  )
