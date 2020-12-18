package lila.forum

import lila.user.User
import org.joda.time.DateTime
import ornicar.scalalib.Random
import scala.concurrent.duration._

case class OldVersion(text: String, createdAt: DateTime)

case class Post(
    _id: String,
    topicId: String,
    categId: String,
    author: Option[String],
    userId: Option[String],
    ip: Option[String],
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

  def showAuthor = (author map (_.trim) filter ("" !=)) | User.anonymous

  def showUserIdOrAuthor = if (erased) "<erased>" else userId | showAuthor

  def isTeam = categId startsWith teamSlug("")

  def updatedOrCreatedAt = updatedAt | createdAt

  def canStillBeEdited =
    updatedOrCreatedAt.plus(permitEditsFor.toMillis).isAfterNow

  def canBeEditedBy(editingId: String): Boolean = userId.fold(false)(editingId == _)

  def shouldShowEditForm(editingId: String) =
    canBeEditedBy(editingId) &&
      updatedOrCreatedAt.plus(showEditFormFor.toMillis).isAfterNow

  def editPost(updated: DateTime, newText: String): Post = {
    val oldVersion = new OldVersion(text, updatedOrCreatedAt)

    // We only store a maximum of 5 historical versions of the post to prevent abuse of storage space
    val history = (oldVersion :: ~editHistory).take(5)

    copy(editHistory = history.some, text = newText, updatedAt = updated.some)
  }

  def hasEdits = editHistory.isDefined

  def displayModIcon = ~modIcon

  def visibleBy(u: Option[User]): Boolean = !troll || u.fold(false)(visibleBy)
  def visibleBy(u: User): Boolean         = !troll || userId.exists(_ == u.id && u.marks.troll)

  def erased = erasedAt.isDefined
}

object Post {

  type ID        = String
  type Reactions = Map[String, Set[User.ID]]

  val idSize = 8

  val reactionsList = List("+1", "-1", "laugh", "thinking", "heart", "bigsby")
  val reactions     = reactionsList.toSet

  def reactionsOf(reactions: Reactions, me: User): Set[String] =
    reactions.view.collect {
      case (reaction, users) if users(me.id) => reaction
    }.toSet

  def make(
      topicId: String,
      categId: String,
      author: Option[String],
      userId: Option[String],
      ip: Option[String],
      text: String,
      number: Int,
      lang: Option[String],
      troll: Boolean,
      hidden: Boolean,
      modIcon: Option[Boolean]
  ): Post = {

    Post(
      _id = Random nextString idSize,
      topicId = topicId,
      author = author,
      userId = userId,
      ip = ip,
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
