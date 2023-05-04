package lila.forum

import ornicar.scalalib.ThreadLocalRandom

import lila.user.User
import lila.security.Granter

case class OldVersion(text: String, createdAt: Instant)

case class ForumPost(
    _id: ForumPostId,
    topicId: ForumTopicId,
    categId: ForumCategId,
    author: Option[String],
    userId: Option[UserId],
    text: String,
    number: Int,
    troll: Boolean,
    lang: Option[String],
    editHistory: Option[List[OldVersion]] = None,
    createdAt: Instant,
    updatedAt: Option[Instant] = None,
    erasedAt: Option[Instant] = None,
    modIcon: Option[Boolean],
    reactions: Option[ForumPost.Reactions] = None
):

  inline def id = _id

  private def showAuthor: String =
    author.map(_.trim).filter("" !=) | (if (~modIcon) User.anonymous.value else User.anonMod)

  def showUserIdOrAuthor: String = if (erased) "<erased>" else userId.fold(showAuthor)(_.value)

  def isTeam = ForumCateg.isTeamSlug(categId)

  def isAnonModPost = !userId.isDefined && ~modIcon

  def updatedOrCreatedAt = updatedAt | createdAt

  def canStillBeEdited =
    updatedOrCreatedAt.plus(ForumPost.permitEditsFor).isAfterNow

  def canBeEditedBy(editingUser: User): Boolean =
    userId match
      case Some(userId) if userId == editingUser.id => true
      case None
          if (Granter(_.PublicMod)(editingUser) || Granter(_.SeeReport)(editingUser)) && isAnonModPost =>
        true
      case _ => false

  def shouldShowEditForm(editingUser: User) =
    canBeEditedBy(editingUser) &&
      updatedOrCreatedAt.plus(ForumPost.showEditFormFor).isAfterNow

  def editPost(updated: Instant, newText: String): ForumPost =
    val oldVersion = OldVersion(text, updatedOrCreatedAt)

    // We only store a maximum of 5 historical versions of the post to prevent abuse of storage space
    val history = (oldVersion :: ~editHistory).take(5)

    copy(
      editHistory = history.some,
      text = newText,
      updatedAt = updated.some,
      reactions = reactions.map(_.view.filterKeys(k => !ForumPost.Reaction.positive(k)).toMap)
    )

  def erase = editPost(nowInstant, "").copy(erasedAt = nowInstant.some)

  def hasEdits = editHistory.isDefined

  def displayModIcon = ~modIcon

  def visibleBy(u: Option[User]): Boolean = !troll || u.exists(visibleBy)
  def visibleBy(u: User): Boolean         = !troll || userId.exists(_ == u.id && u.marks.troll)

  def erased = erasedAt.isDefined

  def isBy(u: User) = userId.exists(_ == u.id)

  override def toString = s"Post($categId/$topicId/$id)"

object ForumPost:

  opaque type Id = String
  object Id extends TotalWrapper[Id, String]

  type Reactions = Map[String, Set[UserId]]

  val idSize                  = 8
  private val permitEditsFor  = 4 hours
  private val showEditFormFor = 3 hours

  object Reaction:
    val PlusOne  = "+1"
    val MinusOne = "-1"
    val Laugh    = "laugh"
    val Thinking = "thinking"
    val Heart    = "heart"
    val Horsey   = "horsey"

    val list: List[String]    = List(PlusOne, MinusOne, Laugh, Thinking, Heart, Horsey)
    val set                   = list.toSet
    val positive: Set[String] = Set(PlusOne, Laugh, Heart, Horsey)

    def of(reactions: Reactions, me: User): Set[String] =
      reactions.view.collect {
        case (reaction, users) if users(me.id) => reaction
      }.toSet

  case class WithFrag(post: ForumPost, body: scalatags.Text.all.Frag)

  def make(
      topicId: ForumTopicId,
      categId: ForumCategId,
      userId: Option[UserId], // anon mod posts
      text: String,
      number: Int,
      lang: Option[String],
      troll: Boolean,
      modIcon: Option[Boolean] = None
  ): ForumPost =
    ForumPost(
      _id = ForumPostId(ThreadLocalRandom nextString idSize),
      topicId = topicId,
      author = none,
      userId = userId,
      text = text,
      number = number,
      lang = lang,
      troll = troll,
      createdAt = nowInstant,
      categId = categId,
      modIcon = modIcon
    )
