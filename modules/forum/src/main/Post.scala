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
    createdAt: DateTime) {

  private val permitEditsFor = 3 hours

  private val coolOffBetweenEdits = 1 minutes

  def id = _id

  def showAuthor = (author map (_.trim) filter ("" !=)) | User.anonymous

  def showUserIdOrAuthor = userId | showAuthor

  def isTeam = categId startsWith teamSlug("")

  def isStaff = categId == "staff"

  def canStillBeEdited(currentTime: DateTime) = {
    createdAt.plus(permitEditsFor.toMillis).isAfter(currentTime)
  }

  def editedTooSoonAfterLastEdit(currentTime: DateTime) = {
    editHistory match {
      case Nil => true
      case lastEdit :: _ => lastEdit.createdAt.plus(coolOffBetweenEdits.toMillis).isAfter(currentTime)
    }
  }

  def canBeEditedBy(editingId: Option[String]) : Boolean = editingId.isDefined && editingId == userId

  def editPost(updated: DateTime, newText: String) : Post = {
    val oldVersion = new OldVersion(text, createdAt)
    val history = oldVersion :: editHistory

    this.copy(editHistory = history, text = newText, createdAt = updated)
  }

  def postHasEdits = !editHistory.isEmpty
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
    hidden: Boolean): Post = Post(
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
    createdAt = DateTime.now,
    categId = categId)
}
