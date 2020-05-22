package lila.forum
package actorApi

case class InsertPost(post: Post)
case class RemovePost(id: String)
case class RemovePosts(ids: List[String])

case class CreatePost(post: Post)
