package lila.forum

import lila.common.Bus
import lila.core.forum.BusForum
import lila.core.perm.Granter as MasterGranter

final class ForumDelete(
    postRepo: ForumPostRepo,
    topicRepo: ForumTopicRepo,
    postApi: ForumPostApi,
    topicApi: ForumTopicApi,
    categApi: ForumCategApi,
    picfitApi: lila.memo.PicfitApi
)(using Executor, akka.stream.Materializer):

  def allByUser(user: User)(using Me): Funit =
    postRepo.unsafe
      .allByUserCursor(user)
      .documentSource()
      .mapAsyncUnordered(4): post =>
        postApi.viewOf(post).flatMap(_.so(deletePost))
      .run()
      .void

  def deleteTopic(view: PostView)(using Me): Funit =
    for
      ids <- postRepo.idsByTopicId(view.topic.id)
      _ <- ids.traverse(id => picfitApi.pullRef(picRef(id)))
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
            _ <- picfitApi.pullRef(picRef(view.post.id))
            _ <- postRepo.remove(view.post)
            _ <- topicApi.denormalize(view.topic)
            _ <- categApi.denormalize(view.categ)
          yield publishDelete(view.post)

  private def publishDelete(p: ForumPost)(using Me) =
    Bus.pub[BusForum](BusForum.RemovePost(p.id, p.userId, p.text, asAdmin = MasterGranter(_.ModerateForum)))
