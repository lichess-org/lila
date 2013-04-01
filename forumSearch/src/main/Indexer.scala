package lila.forumSearch

import lila.forum.PostApi
import lila.forum.actorApi._
import lila.search.{ actorApi ⇒ S }

import akka.actor._
import play.api.libs.concurrent.Execution.Implicits._
import org.elasticsearch.index.query._, FilterBuilders._, QueryBuilders._

private[forumSearch] final class Indexer(
    lowLevel: ActorRef,
    postApi: PostApi) extends Actor {

  def receive = {

    case InsertPost(post) ⇒ postApi liteView post foreach {
      _ foreach { view ⇒
        lowLevel ! S.InsertOne(post.id, Post from view)
      }
    }

    case RemovePost(id)  ⇒ lowLevel ! S.RemoveOne(id)

    case RemoveTopic(id) ⇒ S.RemoveQuery(termQuery(Post.fields.topicId, id))
  }
}
