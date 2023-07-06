package lila.forum

import lila.notify.MentionedInThread

/** Notifier to inform users if they have been mentioned in a post
  *
  * @param notifyApi
  *   Api for sending inbox messages
  */
final class MentionNotifier(
    userRepo: lila.user.UserRepo,
    notifyApi: lila.notify.NotifyApi,
    relationApi: lila.relation.RelationApi,
    prefApi: lila.pref.PrefApi
)(using Executor):

  def notifyMentionedUsers(post: ForumPost, topic: ForumTopic): Funit =
    post.userId.ifFalse(post.troll) so { author =>
      filterValidUsers(extractMentionedUsers(post), author) flatMap { mentionedUsers =>
        mentionedUsers
          .map { user =>
            notifyApi.notifyOne(
              user,
              lila.notify.MentionedInThread(
                mentionedBy = author,
                topicName = topic.name,
                topidId = topic.id,
                category = post.categId,
                postId = post.id
              )
            )
          }
          .parallel
          .void
      }
    }

  /** Checks the database to make sure that the users mentioned exist, and removes any users that do not exist
    * or block the mentioner from the returned list.
    */
  private def filterValidUsers(candidates: Set[UserId], mentionedBy: UserId): Fu[List[UserId]] =
    for
      existingUsers    <- userRepo.filterExists(candidates take 10).map(_.take(5).toSet)
      mentionableUsers <- prefApi.mentionableIds(existingUsers)
      users            <- mentionableUsers.toList.filterA(!relationApi.fetchBlocks(_, mentionedBy))
    yield users

  private def extractMentionedUsers(post: ForumPost): Set[UserId] =
    post.text.contains('@') so {
      val m = lila.common.String.atUsernameRegex.findAllMatchIn(post.text)
      (post.userId foldLeft m.map(_ group 1).map(u => UserStr(u).id).toSet) { _ - _ }
    }
