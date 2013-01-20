package lila
package forum

import search.ElasticSearch._

private[forum] object SearchMapping {

  object fields {
    val body = "bo"
    val topic = "to"
    val author = "au"
  }
  import fields._
  import Mapping._

  def mapping = Map(
    "properties" -> List(
      boost(body, "string", 4),
      boost(topic, "string", 2),
      boost(author, "string")
    ).toMap,
    "analyzer" -> "snowball"
  )

  def apply(view: PostView): Pair[String, Map[String, Any]] = view.post.id -> Map(
    body -> view.post.text,
    topic -> view.topic.name,
    author -> ~(view.post.userId orElse view.post.author)
  )
}
