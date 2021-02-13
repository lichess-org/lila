package lila.forum

import lila.common.Future
import lila.notify.NotifyApi
import lila.notify.{ MentionedInThread, Notification }
import lila.relation.RelationApi
import lila.user.{ User, UserRepo }

/** Notifier to inform users if they have been mentioned in a post
  *
  * @param notifyApi Api for sending inbox messages
  */
final class MentionNotifier(
    userRepo: UserRepo,
    notifyApi: NotifyApi,
    relationApi: RelationApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val forbidden = Set("lichess", "thibault")

  def notifyMentionedUsers(post: Post, topic: Topic): Funit =
    post.userId.ifFalse(post.troll) ?? { author =>
      filterValidUsers(extractMentionedUsers(post), author) flatMap { validUsers =>
        val mentionedBy   = MentionedInThread.MentionedBy(author)
        val notifications = validUsers.map(createMentionNotification(post, topic, _, mentionedBy))
        notifyApi.addNotifications(notifications)
      }
    }

  /** Checks the database to make sure that the users mentioned exist, and removes any users that do not exist
    * or block the mentioner from the returned list.
    */
  private def filterValidUsers(users: Set[User.ID], mentionedBy: User.ID): Fu[List[Notification.Notifies]] = {
    for {
      validUsers <-
        userRepo
          .existingUsernameIds(users take 10 diff forbidden)
          .map(_ take 5)
      validUnblockedUsers <- filterNotBlockedByUsers(validUsers, mentionedBy)
      validNotifies = validUnblockedUsers.map(Notification.Notifies.apply)
    } yield validNotifies
  }

  private def filterNotBlockedByUsers(
      usersMentioned: List[User.ID],
      mentionedBy: User.ID
  ): Fu[List[User.ID]] =
    Future.filterNot(usersMentioned) { relationApi.fetchBlocks(_, mentionedBy) }

  private def createMentionNotification(
      post: Post,
      topic: Topic,
      mentionedUser: Notification.Notifies,
      mentionedBy: MentionedInThread.MentionedBy
  ): Notification = {
    val notificationContent = MentionedInThread(
      mentionedBy,
      MentionedInThread.Topic(topic.name),
      MentionedInThread.TopicId(topic.id),
      MentionedInThread.Category(post.categId),
      MentionedInThread.PostId(post.id)
    )

    Notification.make(mentionedUser, notificationContent)
  }

  private def extractMentionedUsers(post: Post): Set[User.ID] =
    post.text.contains('@') ?? {
      val m = lila.common.String.atUsernameRegex.findAllMatchIn(post.text)
      (post.author foldLeft m.map(_ group 1).map(User.normalize).toSet) { _ - _ }
    }
}
