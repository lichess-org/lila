package lila.forum

import lila.user.{ User, Context }
import lila.common.paginator._
import lila.db.paginator._
import lila.db.Implicits._
import lila.db.api._
import allTubes._

import play.api.libs.concurrent.Execution.Implicits._
import scalaz.{ OptionT, OptionTs }

private[forum] final class TopicApi(env: Env, maxPerPage: Int) extends OptionTs {

  // def show(categSlug: String, slug: String, page: Int): IO[Option[(Categ, Topic, Paginator[Post])]] =
  //   for {
  //     data ← get(categSlug, slug)
  //     _ ← data.fold(io())({ case (_, topic) ⇒ env.topicRepo incViews topic })
  //   } yield data map {
  //     case (categ, topic) ⇒ (categ, topic, env.postApi.paginator(topic, page))
  //   }

  // def makeTopic(
  //   categ: Categ,
  //   data: DataForm.TopicData)(implicit ctx: Context): IO[Topic] = for {
  //   slug ← env.topicRepo.nextSlug(categ, data.name)
  //   topic = Topic(
  //     categId = categ.slug,
  //     slug = slug,
  //     name = data.name)
  //   post = Post(
  //     topicId = topic.id,
  //     author = data.post.author,
  //     userId = ctx.me map (_.id),
  //     ip = ctx.isAnon option ctx.req.remoteAddress,
  //     text = data.post.text,
  //     number = 1,
  //     categId = categ.id)
  //   _ ← env.postRepo saveIO post
  //   // denormalize topic
  //   _ ← env.topicRepo saveIO topic.copy(
  //     nbPosts = 1,
  //     lastPostId = post.id,
  //     updatedAt = post.createdAt)
  //   // denormalize categ
  //   _ ← env.categRepo saveIO categ.copy(
  //     nbTopics = categ.nbTopics + 1,
  //     nbPosts = categ.nbPosts + 1,
  //     lastPostId = post.id)
  //   _ ← env.recent.invalidate
  //   _ ← env.indexer insertOne post
  // } yield topic

  // def get(categSlug: String, slug: String) = for {
  //   categOption ← env.categRepo bySlug categSlug
  //   topicOption ← env.topicRepo.byTree(categSlug, slug)
  // } yield (categOption |@| topicOption).tupled

  // def paginator(categ: Categ, page: Int): Paginator[TopicView] =
  //   Paginator(
  //     SalatAdapter(
  //       dao = env.topicRepo,
  //       query = env.topicRepo byCategQuery categ,
  //       sort = env.topicRepo.sortQuery) map { topic ⇒
  //         env.postRepo byId topic.lastPostId map { post ⇒
  //           TopicView(categ, topic, post, env.postApi lastPageOf topic)
  //         } unsafePerformIO
  //       },
  //     currentPage = page,
  //     maxPerPage = maxPerPage
  //   ) | paginator(categ, 1)

  def delete(categ: Categ, topic: Topic): Funit =
    (PostRepo removeByTopic topic.id) >>
      $remove(topic) >>
      (env.categApi denormalize categ) // >>
  // TODO
  // env.recent.invalidate >>
  //   env.indexer removeTopic topic

  def denormalize(topic: Topic): Funit = for {
    nbPosts ← PostRepo countByTopics List(topic)
    lastPost ← PostRepo lastByTopics List(topic)
    _ ← $update(topic.copy(
      nbPosts = nbPosts,
      lastPostId = lastPost zmap (_.id),
      updatedAt = lastPost.fold(topic.updatedAt)(_.createdAt)
    ))
  } yield ()

  def denormalize: Funit = $find.all[Topic] flatMap { topics ⇒
    topics.map(denormalize).sequence
  } void
}
