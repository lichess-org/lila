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
    deletedByUserIds: Option[List[User.ID]],
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

  def isReplyable = !isTooBig && !isLichess

  def isLichess = creatorId == User.lichessId

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

  def otherUserId(user: User) = if (isCreator(user)) invitedId else creatorId

  def visibleOtherUserId(user: User) =
    if (isCreator(user)) invitedId
    else if (asMod) User.lichessId
    else creatorId

  def senderOf(post: Post) = if (post.isByCreator) creatorId else invitedId

  def visibleSenderOf(post: Post) =
    if (post.isByCreator && asMod) User.lichessId
    else senderOf(post)

  def receiverOf(post: Post) = if (post.isByCreator) invitedId else creatorId

  def visibleReceiverOf(post: Post) =
    if (!post.isByCreator && asMod) User.lichessId
    else receiverOf(post)

  def isWrittenBy(post: Post, user: User) = post.isByCreator == isCreator(user)

  def nonEmptyName = (name.trim.some filter (_.nonEmpty)) | "No subject"

  def deleteFor(user: User) = copy(
    visibleByUserIds = visibleByUserIds filter (user.id !=),
    deletedByUserIds = Some(deletedByUserIds.getOrElse(List()) ::: List(user.id))
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
    deletedByUserIds = None,
    mod = asMod option true
  )

  import lila.db.dsl.BSONJodaDateTimeHandler
  import Post.PostBSONHandler
  private[message] implicit val ThreadBSONHandler =
    lila.db.BSON.LoggingHandler(lila.log("message")) {
      reactivemongo.bson.Macros.handler[Thread]
    }
}
