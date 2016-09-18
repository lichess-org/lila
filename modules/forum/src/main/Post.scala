package lila.forum

import org.joda.time.DateTime
import ornicar.scalalib.Random
import lila.user.User
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
    editHistory: List[OldVersion],
    createdAt: DateTime,
    updatedAt: DateTime) {

  private val permitEditsFor = 4 hours
  private val showEditFormFor = 3 hours

  def id = _id

  def showAuthor = (author map (_.trim) filter ("" !=)) | User.anonymous

  def showUserIdOrAuthor = userId | showAuthor

  def isTeam = categId startsWith teamSlug("")

  def isStaff = categId == "staff"

  def canStillBeEdited() = {
    updatedAt.plus(permitEditsFor.toMillis).isAfter(DateTime.now)
  }

  def canBeEditedBy(editingId: String): Boolean = userId.fold(false)(editingId == _)

  def shouldShowEditForm(editingId: String) =
    canBeEditedBy(editingId) && updatedAt.plus(showEditFormFor.toMillis).isAfter(DateTime.now)

  def editPost(updated: DateTime, newText: String) : Post = {
    val oldVersion = new OldVersion(text, updatedAt)
    val history = oldVersion :: editHistory

    this.copy(editHistory = history, text = newText, updatedAt = updated)
  }

  def hasEdits = editHistory.nonEmpty
}

object Post {

  val idSize = 8

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
    hidden: Boolean): Post = {

    val now = DateTime.now

    Post(
      _id = Random nextStringUppercase idSize,
      topicId = topicId,
      author = author,
      userId = userId,
      ip = ip,
      text = text,
      number = number,
      lang = lang,
      editHistory = List.empty,
      troll = troll,
      hidden = hidden,
      createdAt = now,
      updatedAt = now,
      categId = categId)
  }
}
