package lila.message

import org.joda.time.DateTime
import ornicar.scalalib.Random

import lila.user.User

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
    posts count { post => post.isByInvited && post.isUnRead },
    posts count { post => post.isByCreator && post.isUnRead })

  def nbUnread: Int = posts count (_.isUnRead)

  def firstPostUnreadBy(user: User): Option[Post] = posts find { post =>
    post.isUnRead && post.isByCreator != isCreator(user)
  }

  def userIds = List(creatorId, invitedId)

  def hasUser(user: User) = userIds contains user.id

  def otherUserId(user: User) = isCreator(user).fold(invitedId, creatorId)

  def senderOf(post: Post) = post.isByCreator.fold(creatorId, invitedId)

  def receiverOf(post: Post) = post.isByCreator.fold(invitedId, creatorId)

  def isWrittenBy(post: Post, user: User) = post.isByCreator == isCreator(user)

  def nonEmptyName = (name.trim.some filter (_.nonEmpty)) | "No subject"

  def deleteFor(user: User) = copy(
    visibleByUserIds = visibleByUserIds filter (user.id !=)
  )

  def hasPostsWrittenBy(userId: String) = posts exists (_.isByCreator == (creatorId == userId))

  def endsWith(post: Post) = posts.lastOption ?? post.similar
}

object Thread {

  val idSize = 8

  def make(
    name: String,
    text: String,
    creatorId: String,
    invitedId: String): Thread = Thread(
    id = Random nextStringUppercase idSize,
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

  import lila.db.dsl.BSONJodaDateTimeHandler
  import Post.PostBSONHandler
  private[message] implicit val ThreadBSONHandler =
    lila.db.BSON.LoggingHandler(lila.log("message")) {
      reactivemongo.bson.Macros.handler[Thread]
    }
}
