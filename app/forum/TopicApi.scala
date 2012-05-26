package lila
package forum

import scalaz.effects._
import com.github.ornicar.paginator._

final class TopicApi(env: ForumEnv, maxPerPage: Int) {

  def paginator(categ: Categ, page: Int): Paginator[TopicView] =
    Paginator(
      SalatAdapter(
        dao = env.topicRepo,
        query = env.topicRepo byCategQuery categ,
        sort = env.topicRepo.sortQuery) map { topic ⇒
          env.postRepo byId topic.lastPostId map { post ⇒
            TopicView(categ, topic, post)
          } unsafePerformIO
        },
      currentPage = page,
      maxPerPage = maxPerPage
    ) | paginator(categ, 1)

  def denormalize(topic: Topic): IO[Unit] = for {
    nbPosts ← env.postRepo countByTopics List(topic)
    lastPost ← env.postRepo lastByTopics List(topic)
    _ ← env.topicRepo.saveIO(topic.copy(
      nbPosts = nbPosts,
      lastPostId = lastPost.id
    ))
  } yield ()

  val denormalize: IO[Unit] = for {
    topics ← env.topicRepo.all
    _ ← topics.map(denormalize).sequence
  } yield ()
}
