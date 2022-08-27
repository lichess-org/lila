package lila.forum

import akka.stream.scaladsl._
import reactivemongo.akkastream.{ cursorProducer, AkkaStreamCursor }

import lila.forum.actorApi._
import lila.security.{ Granter => MasterGranter }
import lila.user.{ Holder, User }

final class ForumDelete(
    postRepo: PostRepo,
    topicRepo: TopicRepo,
    categRepo: CategRepo,
    indexer: lila.hub.actors.ForumSearch,
    postApi: PostApi,
    topicApi: TopicApi,
    categApi: CategApi,
    modLog: lila.mod.ModlogApi
)(implicit ec: scala.concurrent.ExecutionContext, mat: akka.stream.Materializer) {

  def post(categSlug: String, postId: String, mod: User): Funit =
    postRepo.unsafe.byCategAndId(categSlug, postId) flatMap {
      _ ?? { post =>
        postApi.viewOf(post) flatMap {
          _ ?? { view =>
            doDelete(view) >> {
              if (MasterGranter(_.ModerateForum)(mod))
                modLog.deletePost(
                  mod.id,
                  post.userId,
                  post.author,
                  text = "%s / %s / %s".format(view.categ.name, view.topic.name, post.text)
                )
              else
                fuccess {
                  logger.info(s"${mod.username} deletes post by ${~post.userId} \"${post.text take 200}\"")
                }
            }
          }
        }
      }
    }

  def allByUser(user: User): Funit =
    postRepo.unsafe
      .allByUserCursor(user)
      .documentSource()
      .mapAsyncUnordered(4) { post =>
        postApi.viewOf(post) flatMap { _ ?? doDelete }
      }
      .toMat(Sink.ignore)(Keep.left)
      .run()
      .void

  private def doDelete(view: PostView) =
    postRepo.isFirstPost(view.topic.id, view.post.id).flatMap {
      case true =>
        postRepo.idsByTopicId(view.topic.id) flatMap { postIds =>
          (postRepo removeByTopic view.topic.id zip topicRepo.remove(view.topic)) >>
            (categApi denormalize view.categ) >>-
            (indexer ! RemovePosts(postIds))
        }
      case false =>
        postRepo.remove(view.post) >>
          (topicApi denormalize view.topic) >>
          (categApi denormalize view.categ) >>-
          (indexer ! RemovePost(view.post.id))
    }
}
