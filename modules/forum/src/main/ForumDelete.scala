package lila.forum

import akka.stream.scaladsl.*

import lila.common.Bus
import lila.core.forum.BusForum
import lila.core.perm.Granter as MasterGranter

final class ForumDelete(
    postRepo: ForumPostRepo,
    topicRepo: ForumTopicRepo,
    postApi: ForumPostApi,
    topicApi: ForumTopicApi,
    categApi: ForumCategApi
)(using Executor, akka.stream.Materializer):

  def allByUser(user: User)(using Me): Funit =
    postRepo.unsafe
      .allByUserCursor(user)
      .documentSource()
      .mapAsyncUnordered(4): post =>
        postApi.viewOf(post).flatMap(_.so(deletePost))
      .runWith(Sink.ignore)
      .void

  def deleteTopic(view: PostView)(using Me): Funit =
    for
      _ <- postRepo.removeByTopic(view.topic.id)
      _ <- topicRepo.remove(view.topic)
      _ <- categApi.denormalize(view.categ)
    yield publishDelete(view.post)

  def deletePost(view: PostView)(using Me): Funit =
    postRepo
      .isFirstPost(view.topic.id, view.post.id)
      .flatMap:
        if _ then deleteTopic(view)
        else
          for
            _ <- postRepo.remove(view.post)
            _ <- topicApi.denormalize(view.topic)
            _ <- categApi.denormalize(view.categ)
          yield publishDelete(view.post)

  private def publishDelete(p: ForumPost)(using Me) =
    Bus.pub[BusForum](BusForum.RemovePost(p.id, p.userId, p.text, asAdmin = MasterGranter(_.ModerateForum)))
