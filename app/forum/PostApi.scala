package lila
package forum

import user.User

import scalaz.effects._
import com.github.ornicar.paginator._
import scala.math.ceil

final class PostApi(env: ForumEnv, maxPerPage: Int) {

  def create(categSlug: String, slug: String, page: Int): IO[Option[(Categ, Topic, Paginator[Post])]] =
    for {
      categOption ← env.categRepo bySlug categSlug
      topicOption ← env.topicRepo.byTree(categSlug, slug)
    } yield categOption |@| topicOption apply {
      case (categ, topic) ⇒ (categ, topic, env.postApi.paginator(topic, page))
    }

  def makePost(
    categ: Categ,
    topic: Topic,
    data: DataForm.PostData,
    user: Option[User]): IO[Post] = for {
    number ← lastNumberOf(topic)
    post = Post(
      topicId = topic.id,
      author = data.author,
      user = user map env.userDbRef,
      text = data.text,
      number = number + 1)
    _ ← env.postRepo saveIO post
    _ ← env.topicApi denormalize topic
    _ ← env.categApi denormalize categ
  } yield post

  def get(postId: String): IO[Option[(Topic, Post)]] = for {
    postOption ← env.postRepo byId postId
    topicOption ← postOption.fold(
      post ⇒ env.topicRepo byId post.topicId,
      io(none[Topic])
    )
  } yield (topicOption |@| postOption).tupled

  def lastNumberOf(topic: Topic): IO[Int] =
    env.postRepo lastByTopics List(topic) map (_.number)

  def pageOf(post: Post) = ceil(post.number / maxPerPage.toFloat).toInt

  def paginator(topic: Topic, page: Int): Paginator[Post] =
    Paginator(
      SalatAdapter(
        dao = env.postRepo,
        query = env.postRepo byTopicQuery topic,
        sort = env.postRepo.sortQuery),
      currentPage = page,
      maxPerPage = maxPerPage
    ) | paginator(topic, 1)
}
