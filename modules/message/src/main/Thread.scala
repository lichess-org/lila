package lila.message

import org.joda.time.DateTime
import ornicar.scalalib.Random

import lila.user.User

case class Thread(
    _id: String,
    name: String,
    createdAt: DateTime,
    updatedAt: DateTime,
    posts: List[Post],
    creatorId: User.ID,
    invitedId: User.ID,
    visibleByUserIds: List[User.ID],
    mod: Option[Boolean]
) {

  def +(post: Post) = copy(
    posts = posts :+ post,
    updatedAt = post.createdAt
  )

  def id = _id

  def asMod = ~mod

  def isCreator(user: User) = creatorId == user.id

  def isReadBy(user: User) = nbUnreadBy(user) == 0

  def isUnReadBy(user: User) = !isReadBy(user)

  private def isPostUnreadBy(user: User)(post: Post) =
    post.isUnRead && post.isByCreator != isCreator(user)

  def nbUnreadBy(user: User): Int = posts count isPostUnreadBy(user)

  def nbPosts = posts.size

  def isTooBig = nbPosts > 200

  def firstPost: Option[Post] = posts.headOption

  def firstPostUnreadBy(user: User): Option[Post] = posts find isPostUnreadBy(user)

  def unreadIndexesBy(user: User): List[Int] = posts.zipWithIndex collect {
    case (post, index) if isPostUnreadBy(user)(post) => index
  }

  def readIndexesBy(user: User): List[Int] = posts.zipWithIndex collect {
    case (post, index) if post.isRead && post.isByCreator != isCreator(user) => index
  }

  def userIds = List(creatorId, invitedId)

  def hasUser(user: User) = userIds contains user.id

  def otherUserId(user: User) = isCreator(user).fold(invitedId, creatorId)

  def visibleOtherUserId(user: User) =
    isCreator(user).fold(invitedId, asMod.fold(Thread.lichess, creatorId))

  def senderOf(post: Post) = post.isByCreator.fold(creatorId, invitedId)

  def visibleSenderOf(post: Post) =
    if (post.isByCreator && asMod) Thread.lichess
    else senderOf(post)

  def receiverOf(post: Post) = post.isByCreator.fold(invitedId, creatorId)

  def visibleReceiverOf(post: Post) =
    if (!post.isByCreator && asMod) Thread.lichess
    else receiverOf(post)

  def isWrittenBy(post: Post, user: User) = post.isByCreator == isCreator(user)

  def nonEmptyName = (name.trim.some filter (_.nonEmpty)) | "No subject"

  def deleteFor(user: User) = copy(
    visibleByUserIds = visibleByUserIds filter (user.id !=)
  )

  def isVisibleBy(userId: User.ID) = visibleByUserIds contains userId

  def isVisibleByOther(user: User) = isVisibleBy(otherUserId(user))

  def hasPostsWrittenBy(userId: User.ID) = posts exists (_.isByCreator == (creatorId == userId))

  def endsWith(post: Post) = posts.lastOption ?? post.similar

  def erase(user: User) = copy(
    posts = posts.map {
      case p if p.isByCreator && user.id == creatorId => p.erase
      case p if !p.isByCreator && user.id == invitedId => p.erase
      case p => p
    }
  )
}

object Thread {

  val idSize = 8

  private val lichess = "lichess"

  def make(
    name: String,
    text: String,
    creatorId: String,
    invitedId: String,
    asMod: Boolean
  ): Thread = Thread(
    _id = Random nextString idSize,
    name = name,
    createdAt = DateTime.now,
    updatedAt = DateTime.now,
    posts = List(Post.make(
      text = text,
      isByCreator = true
    )),
    creatorId = creatorId,
    invitedId = invitedId,
    visibleByUserIds = List(creatorId, invitedId),
    mod = asMod option true
  )

  import lila.db.dsl.BSONJodaDateTimeHandler
  import Post.PostBSONHandler
  private[message] implicit val ThreadBSONHandler =
    lila.db.BSON.LoggingHandler(lila.log("message")) {
      reactivemongo.bson.Macros.handler[Thread]
    }
}
