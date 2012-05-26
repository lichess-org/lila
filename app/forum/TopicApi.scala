package lila
package forum

import scalaz.effects._

final class TopicApi(env: ForumEnv) {

  def denormalize(topic: Topic): IO[Unit] = for {
    nbPosts ← env.postRepo countByTopics List(topic)
    lastPost ← env.postRepo lastByTopics List(topic)
  } yield env.topicRepo.save(topic.copy(
    nbPosts = nbPosts,
    lastPostId = lastPost.id
  ))

  val denormalize: IO[Unit] = for {
    topics ← env.topicRepo.all
    _ ← topics.map(denormalize).sequence
  } yield ()
}
