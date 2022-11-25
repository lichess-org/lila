package lila.forum
package actorApi

case class InsertPost(post: ForumPost)
case class RemovePost(id: String)
case class RemovePosts(ids: List[String])

case class CreatePost(post: ForumPost)
