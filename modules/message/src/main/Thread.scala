package lila.message

import lila.user.User

import org.joda.time.DateTime
import ornicar.scalalib.Random

case class Thread(
    id: String,
    name: String,
    createdAt: DateTime,
    updatedAt: DateTime,
    posts: List[Post],
    creatorId: String,
    invitedId: String,
    visibleByUserIds: List[String]) {

  def +(post: Post) = copy(
    posts = posts :+ post,
    updatedAt = post.createdAt)

  def isCreator(user: User) = creatorId == user.id

  def isReadBy(user: User) = nbUnreadBy(user) == 0

  def isUnReadBy(user: User) = !isReadBy(user)

  def nbUnreadBy(user: User): Int = isCreator(user).fold(
    posts count { post ⇒ post.isByInvited && post.isUnRead },
    posts count { post ⇒ post.isByCreator && post.isUnRead })

  def nbUnread: Int = posts count (_.isUnRead)

  def userIds = List(creatorId, invitedId)

  def hasUser(user: User) = userIds contains user.id

  def otherUserId(user: User) = isCreator(user).fold(invitedId, creatorId)

  def senderOf(post: Post) = post.isByCreator.fold(creatorId, invitedId)

  def receiverOf(post: Post) = post.isByCreator.fold(invitedId, creatorId)

  def nonEmptyName = (name.trim.some filter (_.nonEmpty)) | "No subject"
}

object Thread {

  val idSize = 8

  def make(
    name: String,
    text: String,
    creatorId: String,
    invitedId: String): Thread = Thread(
    id = Random nextString idSize,
    name = name,
    createdAt = DateTime.now,
    updatedAt = DateTime.now,
    posts = List(Post.make(
      text = text,
      isByCreator = true
    )),
    creatorId = creatorId,
    invitedId = invitedId,
    visibleByUserIds = List(creatorId, invitedId))

  import lila.db.Tube
  import Tube.Helpers._
  import play.api.libs.json._

  private[message] lazy val tube = Post.tube |> { implicit pt ⇒
    Tube(
      reader = (__.json update (
        readDate('createdAt) andThen readDate('updatedAt)
      )) andThen Json.reads[Thread],
      writer = Json.writes[Thread],
      writeTransformer = (__.json update (
        writeDate('createdAt) andThen writeDate('updatedAt)
      )).some
    ) 
  }
}
