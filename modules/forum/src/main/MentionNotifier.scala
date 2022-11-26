package lila.forum

import lila.common.Future
import lila.notify.NotifyApi
import lila.notify.{ MentionedInThread, Notification }
import lila.relation.RelationApi
import lila.pref.PrefApi
import lila.user.{ User, UserRepo }

/** Notifier to inform users if they have been mentioned in a post
  *
  * @param notifyApi
  *   Api for sending inbox messages
  */
final class MentionNotifier(
    userRepo: UserRepo,
    notifyApi: NotifyApi,
    relationApi: RelationApi,
    prefApi: PrefApi
)(using ec: scala.concurrent.ExecutionContext):

  def notifyMentionedUsers(post: ForumPost, topic: ForumTopic): Funit =
    post.userId.ifFalse(post.troll) ?? { author =>
      filterValidUsers(extractMentionedUsers(post), author) flatMap { validUsers =>
        val notifications = validUsers.map(createMentionNotification(post, topic, _, UserId(author)))
        notifyApi.addNotifications(notifications)
      }
    }

  /** Checks the database to make sure that the users mentioned exist, and removes any users that do not exist
    * or block the mentioner from the returned list.
    */
  private def filterValidUsers(
      candidates: Set[User.ID],
      mentionedBy: User.ID
  ): Fu[List[Notification.Notifies]] =
    for {
      existingUsers <-
        userRepo
          .existingUsernameIds(candidates take 10)
          .map(_.take(5).toSet)
      mentionableUsers <- prefApi.mentionableIds(existingUsers)
      users <- Future.filterNot(mentionableUsers.toList) { relationApi.fetchBlocks(_, mentionedBy) }
    } yield Notification.Notifies from users

  private def createMentionNotification(
      post: ForumPost,
      topic: ForumTopic,
      mentionedUser: Notification.Notifies,
      mentionedBy: UserId
  ): Notification =
    val notificationContent = MentionedInThread(
      mentionedBy,
      MentionedInThread.Topic(topic.name),
      MentionedInThread.TopicId(topic.id),
      MentionedInThread.Category(post.categId),
      post.id into MentionedInThread.PostId
    )

    Notification.make(mentionedUser into UserId, notificationContent)

  private def extractMentionedUsers(post: ForumPost): Set[User.ID] =
    post.text.contains('@') ?? {
      val m = lila.common.String.atUsernameRegex.findAllMatchIn(post.text)
      (post.userId foldLeft m.map(_ group 1).map(User.normalize).toSet) { _ - _ }
    }
