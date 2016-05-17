package lila.forum

import lila.common.Future

/**
  * Notifier to inform users if they have been mentioned in a post
  *
  * @param messageApi Api for sending inbox messages
  */
final class MentionNotifier(messageApi: lila.message.Api) {

  /**
    * Notify any users mentioned in a post that they have been mentioned
    *
    * @param post The post which may or may not mention users
    * @return
    */
  def notifyMentionedUsers(post: Post, topic: Topic): Fu[Unit] = {

    post.userId match {
      case None => fuccess()
      case Some(author) =>
        val mentionedUsers = extractMentionedUsers(post)
        Future.applySequentially(mentionedUsers)(informOfMention(post, topic, _, author))
    }
  }

  /**
    * Inform user that they have been mentioned by another user
    *
    * @param post          The post that mentions the user
    * @param topic         The topic of the post that mentions the user
    * @param mentionedUser The user that was mentioned
    * @param mentionedBy   The user that mentioned the user
    * @return
    */
  def informOfMention(post: Post, topic: Topic, mentionedUser: String, mentionedBy: String): Fu[Unit] = {
    val inboxNotificationMessage = lila.hub.actorApi.message.LichessThread(
      from = "Lichess",
      to = mentionedUser,
      subject = mentionedBy ++ " mentioned you.",
      message = mentionedBy ++ " mentioned you in the following forum post: " ++
        "http://lichess.org/forum/" ++ post.categId ++ "/" ++ topic.name ++ "#" ++ Integer.toString(post.number))

    messageApi.lichessThread(inboxNotificationMessage)
  }

  /**
    * Pull out any users mentioned in a post
    *
    * @param post The post which may or may not mention users
    * @return
    */
  def extractMentionedUsers(post: Post): List[String] = {
    val postText = post.text

    postText match {
      case postText if postText.contains('@') =>
        val postWords = postText.split(' ')
        postWords.filter(_.startsWith("@")).distinct.map(_.tail).toList
      case _ => List()
    }


  }
}