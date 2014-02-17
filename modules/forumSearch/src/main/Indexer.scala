package lila.forumSearch

import akka.actor._
import org.elasticsearch.index.query._, FilterBuilders._, QueryBuilders._

import lila.forum.actorApi._
import lila.forum.PostApi
import lila.search.{ actorApi => S }

private[forumSearch] final class Indexer(
    lowLevel: ActorRef,
    postApi: PostApi) extends Actor {

  def receive = {

    case InsertPost(post) => postApi liteView post foreach {
      _ foreach { view =>
        lowLevel ! S.InsertOne(post.id, Post from view)
      }
    }

    case RemovePost(id) => lowLevel ! S.RemoveOne(id)

    case RemoveTopic(id) => lowLevel ! S.RemoveQuery(
      termQuery(Post.fields.topicId, id)
    )
  }
}
