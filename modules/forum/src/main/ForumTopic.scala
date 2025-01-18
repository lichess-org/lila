package lila.forum

import reactivemongo.api.bson.Macros.Annotations.Key
import scalalib.ThreadLocalRandom

case class ForumTopic(
    @Key("_id") id: ForumTopicId,
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
    ublogId: Option[UblogPostId] = None
):
  def updatedAt(forUser: Option[User]): Instant =
    if forUser.exists(_.marks.troll) then updatedAtTroll else updatedAt
  def nbPosts(forUser: Option[User]): Int   = if forUser.exists(_.marks.troll) then nbPostsTroll else nbPosts
  def nbReplies(forUser: Option[User]): Int = nbPosts(forUser) - 1
  def lastPostId(forUser: Option[User]): ForumPostId =
    if forUser.exists(_.marks.troll) then lastPostIdTroll else lastPostId

  def open = !closed

  def isTooBig = nbPosts > (if isTeam then 500 else 50)

  def isSticky = ~sticky

  def isAuthor(user: User): Boolean = userId contains user.id
  def isUblog                       = ublogId.isDefined
  def isUblogAuthor(user: User)     = isUblog && isAuthor(user)
  def isTeam                        = ForumCateg.isTeamSlug(categId)

  def withPost(post: ForumPost): ForumTopic =
    copy(
      nbPosts = if post.troll then nbPosts else nbPosts + 1,
      lastPostId = if post.troll then lastPostId else post.id,
      updatedAt = if isTooBig || post.troll then updatedAt else post.createdAt,
      nbPostsTroll = nbPostsTroll + 1,
      lastPostIdTroll = post.id,
      updatedAtTroll = if isTooBig then updatedAt else post.createdAt
    )

  def incNbPosts = copy(nbPosts = nbPosts + 1)

  def isOld = updatedAt.isBefore(nowInstant.minusMonths:
    if isUblog then 12 * 5
    else if isTeam then 6
    else 1)

  def lastPage(maxPerPage: MaxPerPage): Int =
    (nbPosts + maxPerPage.value - 1) / maxPerPage.value

object ForumTopic:

  def nameToId(name: String) =
    val slug = scalalib.StringOps.slug(name)
    // if most chars are not latin, go for random slug
    if slug.lengthIs > (name.lengthIs / 2) then slug else ThreadLocalRandom.nextString(8)

  val idSize = 8

  def make(
      categId: ForumCategId,
      slug: String,
      name: String,
      userId: UserId,
      troll: Boolean = false,
      ublogId: Option[UblogPostId] = None
  ): ForumTopic = ForumTopic(
    id = ForumTopicId(ThreadLocalRandom.nextString(idSize)),
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
