package lila
package forum

import user.User
import http.Context

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
    data: DataForm.PostData)(implicit ctx: Context): IO[Post] = for {
    number ← lastNumberOf(topic)
    post = Post(
      topicId = topic.id,
      author = data.author,
      userId = ctx.me map (_.id),
      ip = ctx.isAnon option ctx.req.remoteAddress,
      text = data.text,
      number = number + 1)
    _ ← env.postRepo saveIO post
    // denormalize topic
    _ ← env.topicRepo saveIO topic.copy(
      nbPosts = topic.nbPosts + 1,
      lastPostId = post.id,
      updatedAt = post.createdAt)
    // denormalize categ
    _ ← env.categRepo saveIO categ.copy(
      nbPosts = categ.nbPosts + 1,
      lastPostId = post.id)
    _ ← env.recent.invalidate
  } yield post

  def get(postId: String): IO[Option[(Topic, Post)]] = for {
    postOption ← env.postRepo byId postId
    topicOption ← postOption.fold(
      post ⇒ env.topicRepo byId post.topicId,
      io(none[Topic])
    )
  } yield (topicOption |@| postOption).tupled

  def view(post: Post): IO[Option[PostView]] = for {
    topicOption ← env.topicRepo byId post.topicId
    categOption ← topicOption.fold(
      topic ⇒ env.categRepo bySlug topic.categId,
      io(none[Categ])
    )
  } yield topicOption |@| categOption apply {
    case (topic, categ) ⇒ PostView(post, topic, categ, lastPageOf(topic))
  }

  def lastNumberOf(topic: Topic): IO[Int] =
    env.postRepo lastByTopics List(topic) map (_.number)

  def lastPageOf(topic: Topic) = 
    ceil(topic.nbPosts / maxPerPage.toFloat).toInt

  def paginator(topic: Topic, page: Int): Paginator[Post] =
    Paginator(
      SalatAdapter(
        dao = env.postRepo,
        query = env.postRepo byTopicQuery topic,
        sort = env.postRepo.sortQuery),
      currentPage = page,
      maxPerPage = maxPerPage
    ) | paginator(topic, 1)

  def delete(postId: String): IO[Unit] = for {
    postOption ← env.postRepo byId postId
    viewOption ← postOption.fold(view, io(none))
    _ ← viewOption.fold(
      view ⇒ (view.topic.nbPosts == 1).fold(
        env.topicApi.delete(view.categ, view.topic),
        for {
          _ ← env.postRepo removeIO view.post
          _ ← env.topicApi denormalize view.topic
          _ ← env.categApi denormalize view.categ
          _ ← env.recent.invalidate
        } yield ()
      ),
      io()
    )
  } yield ()

}
