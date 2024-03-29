package lila.hub
package forum

trait ForumPost:
  val id: ForumPostId
  val text: String
  // val topicId: ForumTopicId
  // val categId: ForumCategId
  // val author: Option[String]
  // val userId: Option[UserId]
  // val number: Int
  // val troll: Boolean
  // val lang: Option[String]
  // val createdAt: Instant
  // val updatedAt: Option[Instant] = None
  // val erasedAt: Option[Instant]  = None

trait ForumPostApi:
  def getPost(postId: ForumPostId): Fu[Option[ForumPost]]
