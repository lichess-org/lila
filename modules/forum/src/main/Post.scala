package lila.forum

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.user.User
import lila.security.Granter

case class OldVersion(text: String, createdAt: DateTime)

case class Post(
    _id: String,
    topicId: String,
    categId: String,
    author: Option[String],
    userId: Option[String],
    text: String,
    number: Int,
    troll: Boolean,
    hidden: Boolean,
    lang: Option[String],
    editHistory: Option[List[OldVersion]] = None,
    createdAt: DateTime,
    updatedAt: Option[DateTime] = None,
    erasedAt: Option[DateTime] = None,
    modIcon: Option[Boolean],
    reactions: Option[Post.Reactions] = None
) {

  private val permitEditsFor  = 4 hours
  private val showEditFormFor = 3 hours

  def id = _id

  def showAuthor = (author map (_.trim) filter ("" !=)) | (if (~modIcon) User.anonymous else User.anonMod)

  def showUserIdOrAuthor = if (erased) "<erased>" else userId | showAuthor

  def isTeam = categId startsWith teamSlug("")

  def isAnonModPost = !userId.isDefined && ~modIcon

  def updatedOrCreatedAt = updatedAt | createdAt

  def canStillBeEdited =
    updatedOrCreatedAt.plus(permitEditsFor.toMillis).isAfterNow

  def canBeEditedBy(editingUser: User): Boolean =
    userId match {
      case Some(userId) if userId == editingUser.id => true
      case None
          if (Granter(_.PublicMod)(editingUser) || Granter(_.SeeReport)(editingUser)) && isAnonModPost =>
        true
      case _ => false
    }

  def shouldShowEditForm(editingUser: User) =
    canBeEditedBy(editingUser) &&
      updatedOrCreatedAt.plus(showEditFormFor.toMillis).isAfterNow

  def editPost(updated: DateTime, newText: String): Post = {
    val oldVersion = OldVersion(text, updatedOrCreatedAt)

    // We only store a maximum of 5 historical versions of the post to prevent abuse of storage space
    val history = (oldVersion :: ~editHistory).take(5)

    copy(
      editHistory = history.some,
      text = newText,
      updatedAt = updated.some,
      reactions = reactions.map(_.view.filterKeys(k => !Post.Reaction.positive(k)).toMap)
    )
  }

  def erase = editPost(DateTime.now, "").copy(erasedAt = DateTime.now.some)

  def hasEdits = editHistory.isDefined

  def displayModIcon = ~modIcon

  def visibleBy(u: Option[User]): Boolean = !troll || u.exists(visibleBy)
  def visibleBy(u: User): Boolean         = !troll || userId.exists(_ == u.id && u.marks.troll)

  def erased = erasedAt.isDefined

  def isBy(u: User) = userId.exists(_ == u.id)
}

object Post {

  type ID        = String
  type Reactions = Map[String, Set[User.ID]]

  val idSize = 8

  object Reaction {
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
  }

  def make(
      topicId: String,
      categId: String,
      author: Option[String],
      userId: Option[User.ID],
      text: String,
      number: Int,
      lang: Option[String],
      troll: Boolean,
      hidden: Boolean,
      modIcon: Option[Boolean]
  ): Post = {

    Post(
      _id = lila.common.ThreadLocalRandom nextString idSize,
      topicId = topicId,
      author = author,
      userId = userId,
      text = text,
      number = number,
      lang = lang,
      troll = troll,
      hidden = hidden,
      createdAt = DateTime.now,
      categId = categId,
      modIcon = modIcon
    )
  }
}
