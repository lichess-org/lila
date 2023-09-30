package lila.forum

import ornicar.scalalib.ThreadLocalRandom

import lila.user.{ Me, User }
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
    author.map(_.trim).filter("" !=) | (if ~modIcon then User.anonymous.value else User.anonMod)

  def showUserIdOrAuthor: String = if erased then "<erased>" else userId.fold(showAuthor)(_.value)

  def isTeam = ForumCateg.isTeamSlug(categId)

  def isAnonModPost = !userId.isDefined && ~modIcon

  def updatedOrCreatedAt = updatedAt | createdAt

  def canStillBeEdited =
    updatedOrCreatedAt.plus(ForumPost.permitEditsFor).isAfterNow

  def canBeEditedByMe(using me: Me): Boolean =
    userId match
      case Some(userId) if me is userId => true
      case None if (Granter(_.PublicMod) || Granter(_.SeeReport)) && isAnonModPost =>
        true
      case _ => false

  def shouldShowEditForm(using Me) =
    canBeEditedByMe && updatedOrCreatedAt.plus(ForumPost.showEditFormFor).isAfterNow

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

  type Reactions = Map[Reaction, Set[UserId]]

  val idSize                  = 8
  private val permitEditsFor  = 4 hours
  private val showEditFormFor = 3 hours

  enum Reaction(val key: String):
    case PlusOne  extends Reaction("+1")
    case MinusOne extends Reaction("-1")
    case Laugh    extends Reaction("laugh")
    case Thinking extends Reaction("thinking")
    case Heart    extends Reaction("heart")
    case Horsey   extends Reaction("horsey")
    override def toString = key
  object Reaction:
    val list               = values.toList
    val set                = values.toSet
    val positive           = Set(PlusOne, Laugh, Heart, Horsey)
    def apply(key: String) = list.find(_.key == key)
    def of(reactions: Reactions, me: User): Set[Reaction] =
      reactions.view
        .collect:
          case (reaction, users) if users(me.id) => reaction
        .toSet

  case class WithFrag(post: ForumPost, body: scalatags.Text.all.Frag, hide: Boolean = false)

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
