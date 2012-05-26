package lila
package forum

import scalaz.effects._

final class CategApi(env: ForumEnv) {

  val list: IO[List[CategView]] = for {
    categs ← env.categRepo.all
    views ← (categs map { categ ⇒
      env.postRepo byId categ.lastPostId map { post ⇒
        CategView(categ, post)
      }
    }).sequence
  } yield views

  def denormalize(categ: Categ): IO[Unit] = for {
    topics ← env.topicRepo byCateg categ
    nbPosts ← env.postRepo countByTopics topics
    lastPost ← env.postRepo lastByTopics topics
    _ ← env.categRepo.saveIO(categ.copy(
      nbTopics = topics.size,
      nbPosts = nbPosts,
      lastPostId = lastPost.id
    ))
  } yield ()

  val denormalize: IO[Unit] = for {
    categs ← env.categRepo.all
    _ ← categs.map(denormalize).sequence
  } yield ()
}
