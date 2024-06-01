package lila.forumSearch
import play.api.libs.json.*

private case class Query(text: String, troll: Boolean):
  def transform = lila.search.spec.Query.forum(text, troll)
