package lila
package forum

import search.ElasticSearch._

private[forum] object SearchMapping {

  object fields {
    val body = "bo"
    val topic = "to"
    val topicId = "ti"
    val author = "au"
    val staff = "st"
  }
  import fields._
  import Mapping._

  def mapping = Map(
    "properties" -> List(
      boost(body, "string", 2),
      boost(topic, "string", 4),
      boost(author, "string"),
      field(topicId, "string"),
      field(staff, "boolean")
    ).toMap,
    "analyzer" -> "snowball"
  )

  def apply(view: PostLiteView): Pair[String, Map[String, Any]] = view.post.id -> Map(
    body -> view.post.text,
    topic -> view.topic.name,
    author -> ~(view.post.userId orElse view.post.author),
    topicId -> view.topic.id,
    staff -> view.post.isStaff
  )
}
