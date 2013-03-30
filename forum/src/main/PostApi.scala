package lila.forum

import lila.user.{ User, Context }
import lila.common.paginator._
import lila.db.paginator._
import lila.db.Implicits._
import lila.db.api._

import play.api.libs.concurrent.Execution.Implicits._

final class PostApi(env: Env, maxPerPage: Int) {

  private implicit def tube = postTube

  def create(categSlug: String, slug: String, page: Int): Fu[Option[(Categ, Topic, Paginator[Post])]] = for {
    categOption ← CategRepo bySlug categSlug
    topicOption ← TopicRepo.byTree(categSlug, slug)
    tuple ← (categOption |@| topicOption).tupled zmap {
      case (categ, topic) ⇒ paginator(topic, page) map { (categ, topic, _).some }
    }
  } yield tuple

  // def makePost(
  //   categ: Categ,
  //   topic: Topic,
  //   data: DataForm.PostData)(implicit ctx: Context): IO[Post] = for {
  //   number ← lastNumberOf(topic)
  //   post = Post(
  //     topicId = topic.id,
  //     author = data.author,
  //     userId = ctx.me map (_.id),
  //     ip = ctx.isAnon option ctx.req.remoteAddress,
  //     text = data.text,
  //     number = number + 1,
  //     categId = categ.id)
  //   _ ← env.postRepo saveIO post
  //   // denormalize topic
  //   _ ← env.topicRepo saveIO topic.copy(
  //     nbPosts = topic.nbPosts + 1,
  //     lastPostId = post.id,
  //     updatedAt = post.createdAt)
  //   // denormalize categ
  //   _ ← env.categRepo saveIO categ.copy(
  //     nbPosts = categ.nbPosts + 1,
  //     lastPostId = post.id)
  //   _ ← env.recent.invalidate
  //   _ ← env.indexer insertOne post
  // } yield post

  // def get(postId: String): IO[Option[(Topic, Post)]] = for {
  //   postOption ← env.postRepo byId postId
  //   topicOption ← postOption.fold(io(none[Topic]))(post ⇒ env.topicRepo byId post.topicId)
  // } yield (topicOption |@| postOption).tupled

  // def views(posts: List[Post]): IO[List[PostView]] = for {
  //   topics ← env.topicRepo byIds posts.map(_.topicId).distinct
  //   categs ← env.categRepo byIds topics.map(_.categId).distinct
  // } yield (for {
  //   post ← posts
  // } yield for {
  //   topic ← topics find (_.id == post.topicId)
  //   categ ← categs find (_.slug == topic.categId)
  // } yield PostView(post, topic, categ, lastPageOf(topic))
  // ).flatten

  // def viewsFromIds(postIds: List[String]): IO[List[PostView]] =
  //   env.postRepo byOrderedIds postIds flatMap views

  // def view(post: Post): IO[Option[PostView]] = views(List(post)) map (_.headOption)

  // def liteViews(posts: List[Post]): IO[List[PostLiteView]] = for {
  //   topics ← env.topicRepo byIds posts.map(_.topicId).distinct
  // } yield (for {
  //   post ← posts
  // } yield for {
  //   topic ← topics find (_.id == post.topicId)
  // } yield PostLiteView(post, topic, lastPageOf(topic))
  // ).flatten

  // def liteView(post: Post): IO[Option[PostLiteView]] = liteViews(List(post)) map (_.headOption)

  def lastNumberOf(topic: Topic): Fu[Int] =
    PostRepo lastByTopics List(topic) map { _ zmap (_.number) }

  def lastPageOf(topic: Topic) =
    math.ceil(topic.nbPosts / maxPerPage.toFloat).toInt

  def paginator(topic: Topic, page: Int): Fu[Paginator[Post]] = Paginator(
    new Adapter(
      selector = PostRepo selectTopic topic,
      sort = PostRepo.sortQuery :: Nil),
    currentPage = page,
    maxPerPage = maxPerPage)

  // def delete(postId: String, mod: User): IO[Unit] = for {
  //   postOption ← env.postRepo byId postId
  //   viewOption ← ~postOption.map(view)
  //   _ ← ~viewOption.map(view ⇒ for {
  //     deleteTopic ← env.postRepo.isFirstPost(view.topic.id, view.post.id)
  //     _ ← deleteTopic.fold(
  //       env.topicApi.delete(view.categ, view.topic),
  //       for {
  //         _ ← env.postRepo removeIO view.post
  //         _ ← env.topicApi denormalize view.topic
  //         _ ← env.categApi denormalize view.categ
  //         _ ← env.recent.invalidate
  //       } yield ()
  //     )
  //     post = view.post
  //     _ ← modLog.deletePost(mod, post.userId, post.author, post.ip,
  //       text = "%s / %s / %s".format(view.categ.name, view.topic.name, post.text))
  //     _ ← env.indexer removeOne post
  //   } yield ())
  // } yield ()
}
