package lila.forum

import lila.notify.{Notification, MentionedInThread}
import lila.notify.NotifyApi
import lila.relation.RelationApi
import lila.user.{UserRepo, User}
import org.joda.time.DateTime
import lila.common.Future

/**
  * Notifier to inform users if they have been mentioned in a post
  *
  * @param notifyApi Api for sending inbox messages
  */
final class MentionNotifier(notifyApi: NotifyApi, relationApi: RelationApi) {

  def notifyMentionedUsers(post: Post, topic: Topic): Unit = {
    post.userId foreach { author =>
      val mentionedUsers = extractMentionedUsers(post)
      val mentionedBy = MentionedInThread.MentionedBy(author)

      for {
        validUsers <- filterValidUsers(mentionedUsers, author)
        notifications = validUsers.map(createMentionNotification(post, topic, _, mentionedBy))
      } yield notifyApi.addNotifications(notifications)
    }
  }

  /**
    * Checks the database to make sure that the users mentioned exist, and removes any users that do not exist
    * or block the mentioner from the returned list.
    */
  private def filterValidUsers(users: Set[String], mentionedBy: String) : Fu[List[Notification.Notifies]] = {
    for {
      validUsers <- UserRepo.existingUsernameIds(users take 20).map(_.take(5))
      validUnblockedUsers <- filterNotBlockedByUsers(validUsers, mentionedBy)
      validNotifies = validUnblockedUsers.map(Notification.Notifies.apply)
    } yield validNotifies
  }

  private def filterNotBlockedByUsers(usersMentioned: List[String], mentionedBy: String) : Fu[List[String]]= {
    Future.filterNot(usersMentioned)(mentioned => relationApi.fetchBlocks(mentioned, mentionedBy))
  }

  private def createMentionNotification(post: Post, topic: Topic, mentionedUser: Notification.Notifies, mentionedBy: MentionedInThread.MentionedBy): Notification = {
    val notificationContent = MentionedInThread(
        mentionedBy,
        MentionedInThread.Topic(topic.name),
        MentionedInThread.Category(post.categId),
        MentionedInThread.PostId(post.id))

    Notification(mentionedUser, notificationContent, Notification.NotificationRead(false), DateTime.now)
  }

  private def extractMentionedUsers(post: Post): Set[String] = {
    User.atUsernameRegex.findAllMatchIn(post.text).map(_.matched.tail).toSet
  }
}