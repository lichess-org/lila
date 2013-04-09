package lila.forum

import lila.user.{ User, Context }
import lila.common.paginator._
import lila.db.paginator._
import lila.db.Implicits._
import lila.db.api._
import tube._
import actorApi._

import akka.actor.ActorRef
import play.api.libs.json.JsObject
import play.api.libs.concurrent.Execution.Implicits._
import scalaz.{ OptionT, OptionTs }

final class PostApi(env: Env, indexer: ActorRef, maxPerPage: Int) extends OptionTs {

  def create(categSlug: String, slug: String, page: Int): Fu[Option[(Categ, Topic, Paginator[Post])]] = for {
    categ ← optionT(CategRepo bySlug categSlug)
    topic ← optionT(TopicRepo.byTree(categSlug, slug))
    tuple ← optionT(paginator(topic, page) map { (categ, topic, _).some })
  } yield tuple

  def makePost(
    categ: Categ,
    topic: Topic,
    data: DataForm.PostData)(implicit ctx: Context): Fu[Post] =
    lastNumberOf(topic) flatMap { number ⇒
      val post = Post.make(
        topicId = topic.id,
        author = data.author,
        userId = ctx.me map (_.id),
        ip = ctx.isAnon option ctx.req.remoteAddress,
        text = data.text,
        number = number + 1,
        categId = categ.id)
      $insert(post) >>
        // denormalize topic
        $update(topic.copy(
          nbPosts = topic.nbPosts + 1,
          lastPostId = post.id,
          updatedAt = post.createdAt)) >>
        // denormalize categ
        $update(categ.copy(
          nbPosts = categ.nbPosts + 1,
          lastPostId = post.id)) >>
        (indexer ! InsertPost(post)) >>
        env.recent.invalidate inject post
    }

  def get(postId: String): Fu[Option[(Topic, Post)]] = for {
    post ← optionT($find.byId[Post](postId))
    topic ← optionT($find.byId[Topic](post.topicId))
  } yield topic -> post

  def views(posts: List[Post]): Fu[List[PostView]] = for {
    topics ← $find.byIds[Topic](posts.map(_.topicId).distinct)
    categs ← $find.byIds[Categ](topics.map(_.categId).distinct)
  } yield posts map { post ⇒
    for {
      topic ← topics find (_.id == post.topicId)
      categ ← categs find (_.slug == topic.categId)
    } yield PostView(post, topic, categ, lastPageOf(topic))
  } flatten

  def viewsFromIds(postIds: List[String]): Fu[List[PostView]] =
    $find.byOrderedIds[Post](postIds) flatMap views

  def view(post: Post): Fu[Option[PostView]] =
    views(List(post)) map (_.headOption)

  def liteViews(posts: List[Post]): Fu[List[PostLiteView]] = for {
    topics ← $find.byIds[Topic](posts.map(_.topicId).distinct)
  } yield posts map { post ⇒
    topics find (_.id == post.topicId) map { topic ⇒
      PostLiteView(post, topic, lastPageOf(topic))
    }
  } flatten

  def liteView(post: Post): Fu[Option[PostLiteView]] =
    liteViews(List(post)) map (_.headOption)

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

  def delete(postId: String, mod: User): Funit = (for {
    post ← optionT($find.byId[Post](postId))
    view ← optionT(view(post))
    _ ← optionT(for {
      first ← PostRepo.isFirstPost(view.topic.id, view.post.id)
      _ ← first.fold(
        env.topicApi.delete(view.categ, view.topic),
        $remove[Post](view.post) >>
          (env.topicApi denormalize view.topic) >>
          (env.categApi denormalize view.categ) >>
          env.recent.invalidate >>
          (indexer ! RemovePost(post)))
      // TODO
      // _ ← modLog.deletePost(mod, post.userId, post.author, post.ip,
      //   text = "%s / %s / %s".format(view.categ.name, view.topic.name, post.text))
    } yield true.some)
  } yield ()).value.void

  def cursor(selector: JsObject) = $query[Post](selector).cursor[Option[Post]]
}
