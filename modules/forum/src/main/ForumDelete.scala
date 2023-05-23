package lila.forum

import akka.stream.scaladsl.*

import lila.security.{ Granter as MasterGranter }
import lila.user.User

final class ForumDelete(
    postRepo: ForumPostRepo,
    topicRepo: ForumTopicRepo,
    indexer: lila.hub.actors.ForumSearch,
    postApi: ForumPostApi,
    topicApi: ForumTopicApi,
    categApi: ForumCategApi,
    modLog: lila.mod.ModlogApi
)(using Executor, akka.stream.Materializer):

  def post(categId: ForumCategId, postId: ForumPostId, mod: User): Funit =
    postRepo.unsafe.byCategAndId(categId, postId) flatMapz { post =>
      postApi.viewOf(post) flatMapz { view =>
        doDelete(view) >> {
          if (MasterGranter(_.ModerateForum)(mod))
            modLog.deletePost(
              mod.id into ModId,
              post.userId,
              text = "%s / %s / %s".format(view.categ.name, view.topic.name, post.text)
            )
          else
            fuccess {
              logger.info(
                s"${mod.username} deletes post by ${post.userId.??(_.value)} \"${post.text take 200}\""
              )
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
      if _ then
        postRepo.idsByTopicId(view.topic.id) flatMap { postIds =>
          (postRepo removeByTopic view.topic.id zip topicRepo.remove(view.topic)) >>
            (categApi denormalize view.categ) >>-
            (indexer ! RemovePosts(postIds))
        }
      else
        postRepo.remove(view.post) >>
          (topicApi denormalize view.topic) >>
          (categApi denormalize view.categ) >>-
          (indexer ! RemovePost(view.post.id))
    }
