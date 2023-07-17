package lila.forum

import akka.stream.scaladsl.*

import lila.security.{ Granter as MasterGranter }
import lila.user.{ User, Me }

final class ForumDelete(
    postRepo: ForumPostRepo,
    topicRepo: ForumTopicRepo,
    indexer: lila.hub.actors.ForumSearch,
    postApi: ForumPostApi,
    topicApi: ForumTopicApi,
    categApi: ForumCategApi,
    modLog: lila.mod.ModlogApi
)(using Executor, akka.stream.Materializer):

  def post(categId: ForumCategId, postId: ForumPostId)(using mod: Me): Funit =
    postRepo.unsafe.byCategAndId(categId, postId) flatMapz { post =>
      postApi.viewOf(post) flatMapz { view =>
        doDelete(view) >> {
          if MasterGranter(_.ModerateForum)
          then
            modLog.deletePost(
              post.userId,
              text = "%s / %s / %s".format(view.categ.name, view.topic.name, post.text)
            )
          else
            fuccess:
              logger.info:
                s"${mod.username} deletes post by ${post.userId.so(_.value)} \"${post.text take 200}\""
        }
      }
    }

  def allByUser(user: User): Funit =
    postRepo.unsafe
      .allByUserCursor(user)
      .documentSource()
      .mapAsyncUnordered(4) { post =>
        postApi.viewOf(post) flatMap { _ so doDelete }
      }
      .runWith(Sink.ignore)
      .void

  private def doDelete(view: PostView) =
    postRepo.isFirstPost(view.topic.id, view.post.id).flatMap {
      if _ then
        for
          postIds <- postRepo.idsByTopicId(view.topic.id)
          _       <- postRepo removeByTopic view.topic.id
          _       <- topicRepo.remove(view.topic)
          _       <- categApi denormalize view.categ
        yield indexer ! RemovePosts(postIds)
      else
        for
          _ <- postRepo.remove(view.post)
          _ <- topicApi.denormalize(view.topic)
          _ <- categApi.denormalize(view.categ)
        yield indexer ! RemovePost(view.post.id)
    }
