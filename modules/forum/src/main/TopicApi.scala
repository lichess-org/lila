package lila.forum

import lila.user.{ User, Context }
import lila.common.paginator._
import lila.db.paginator._
import lila.db.Implicits._
import lila.db.api._
import tube._
import actorApi._

import akka.actor.ActorRef
import scalaz.{ OptionT, OptionTs }

private[forum] final class TopicApi(
  env: Env, 
  indexer: lila.hub.ActorLazyRef,
  maxPerPage: Int) extends OptionTs {

  def show(categSlug: String, slug: String, page: Int): Fu[Option[(Categ, Topic, Paginator[Post])]] =
    for {
      data ← get(categSlug, slug)
      res ← data zmap {
        case (categ, topic) ⇒ (TopicRepo incViews topic) >>
          (env.postApi.paginator(topic, page) map { (categ, topic, _).some })
      }
    } yield res

  def makeTopic(
    categ: Categ,
    data: DataForm.TopicData)(implicit ctx: Context): Fu[Topic] =
    TopicRepo.nextSlug(categ, data.name) flatMap { slug ⇒
      val topic = Topic.make(
        categId = categ.slug,
        slug = slug,
        name = data.name)
      val post = Post.make(
        topicId = topic.id,
        author = data.post.author,
        userId = ctx.me map (_.id),
        ip = ctx.isAnon option ctx.req.remoteAddress,
        text = data.post.text,
        number = 1,
        categId = categ.id)
      $insert(post) >>
        $insert(topic.copy(
          nbPosts = 1,
          lastPostId = post.id,
          updatedAt = post.createdAt)) >>
        $update(categ.copy(
          nbTopics = categ.nbTopics + 1,
          nbPosts = categ.nbPosts + 1,
          lastPostId = post.id)) >>-
        (indexer ! InsertPost(post)) >>
        env.recent.invalidate inject topic
    }

  def get(categSlug: String, slug: String): Fu[Option[(Categ, Topic)]] = for {
    categ ← optionT(CategRepo bySlug categSlug)
    topic ← optionT(TopicRepo.byTree(categSlug, slug))
  } yield categ -> topic

  def paginator(categ: Categ, page: Int): Fu[Paginator[TopicView]] = Paginator(
    adapter = new Adapter[Topic](
      selector = TopicRepo byCategQuery categ,
      sort = Seq($sort.createdDesc)
    ) mapFuture { topic ⇒
      $find.byId[Post](topic.lastPostId) map { post ⇒
        TopicView(categ, topic, post, env.postApi lastPageOf topic)
      }
    },
    currentPage = page,
    maxPerPage = maxPerPage)

  def delete(categ: Categ, topic: Topic): Funit =
    (PostRepo removeByTopic topic.id) >>
      $remove(topic) >>
      (env.categApi denormalize categ) >>-
      (indexer ! RemoveTopic(topic.id)) >>
      env.recent.invalidate

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
