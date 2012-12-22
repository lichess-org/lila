package lila
package forum

import user.User
import http.Context

import scalaz.effects._
import com.github.ornicar.paginator._

final class TopicApi(env: ForumEnv, maxPerPage: Int) {

  def show(categSlug: String, slug: String, page: Int): IO[Option[(Categ, Topic, Paginator[Post])]] =
    for {
      data ← get(categSlug, slug)
      _ ← data.fold({
        case (_, topic) ⇒ env.topicRepo incViews topic
      }, io())
    } yield data map {
      case (categ, topic) ⇒ (categ, topic, env.postApi.paginator(topic, page))
    }

  def makeTopic(
    categ: Categ,
    data: DataForm.TopicData)(implicit ctx: Context): IO[Topic] = for {
    slug ← env.topicRepo.nextSlug(categ, data.name)
    topic = Topic(
      categId = categ.slug,
      slug = slug,
      name = data.name)
    post = Post(
      topicId = topic.id,
      author = data.post.author,
      userId = ctx.me map (_.id),
      ip = ctx.isAnon option ctx.req.remoteAddress,
      text = data.post.text,
      number = 1,
      categId = categ.id)
    _ ← env.postRepo saveIO post
    // denormalize topic
    _ ← env.topicRepo saveIO topic.copy(
      nbPosts = 1,
      lastPostId = post.id,
      updatedAt = post.createdAt)
    // denormalize categ
    _ ← env.categRepo saveIO categ.copy(
      nbTopics = categ.nbTopics + 1,
      nbPosts = categ.nbPosts + 1,
      lastPostId = post.id)
    _ ← env.recent.invalidate
  } yield topic

  def get(categSlug: String, slug: String) = for {
    categOption ← env.categRepo bySlug categSlug
    topicOption ← env.topicRepo.byTree(categSlug, slug)
  } yield (categOption |@| topicOption).tupled

  def paginator(categ: Categ, page: Int): Paginator[TopicView] =
    Paginator(
      SalatAdapter(
        dao = env.topicRepo,
        query = env.topicRepo byCategQuery categ,
        sort = env.topicRepo.sortQuery) map { topic ⇒
          env.postRepo byId topic.lastPostId map { post ⇒
            TopicView(categ, topic, post, env.postApi lastPageOf topic)
          } unsafePerformIO
        },
      currentPage = page,
      maxPerPage = maxPerPage
    ) | paginator(categ, 1)

  def delete(categ: Categ, topic: Topic): IO[Unit] = for {
    _ ← env.postRepo removeByTopicId topic.id
    _ ← env.topicRepo removeIO topic
    _ ← env.categApi denormalize categ
    _ ← env.recent.invalidate
  } yield ()

  def denormalize(topic: Topic): IO[Unit] = for {
    nbPosts ← env.postRepo countByTopics List(topic)
    lastPost ← env.postRepo lastByTopics List(topic)
    _ ← env.topicRepo.saveIO(topic.copy(
      nbPosts = nbPosts,
      lastPostId = lastPost.id,
      updatedAt = lastPost.createdAt
    ))
  } yield ()

  val denormalize: IO[Unit] = for {
    topics ← env.topicRepo.all
    _ ← topics.map(denormalize).sequence
  } yield ()
}
