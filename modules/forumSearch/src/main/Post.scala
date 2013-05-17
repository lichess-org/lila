package lila.forumSearch

import lila.forum.PostLiteView
import lila.search.ElasticSearch
import play.api.libs.json._

private[forumSearch] object Post {

  object fields {
    val body = "bo"
    val topic = "to"
    val topicId = "ti"
    val author = "au"
    val staff = "st"
    val troll = "tr"
  }
  import fields._
  import ElasticSearch.Mapping._

  def jsonMapping = Json.obj(
    "properties" -> Json.toJson(List(
      boost(body, "string", 2),
      boost(topic, "string", 4),
      boost(author, "string"),
      field(topicId, "string"),
      field(staff, "boolean"),
      field(troll, "boolean")
    ).toMap),
    "analyzer" -> "snowball"
  )

  def from(view: PostLiteView): JsObject = Json.obj(
    body -> view.post.text,
    topic -> view.topic.name,
    author -> ~(view.post.userId orElse view.post.author),
    topicId -> view.topic.id,
    staff -> view.post.isStaff,
    troll -> view.post.troll
  )
}
