package lila.forum

import lila.user.{ User, Context }
import lila.common.paginator._
import lila.db.paginator._
import lila.db.Implicits._
import lila.db.api._
import tube._
import actorApi._

import scalaz.{ OptionT, OptionTs }

private[forum] final class TopicApi(
    env: Env,
    indexer: lila.hub.ActorLazyRef,
    maxPerPage: Int,
    modLog: lila.mod.ModlogApi) extends OptionTs {

  def show(categSlug: String, slug: String, page: Int, troll: Boolean): Fu[Option[(Categ, Topic, Paginator[Post])]] =
    for {
      data ← (for {
        categ ← optionT(CategRepo bySlug categSlug)
        topic ← optionT(TopicRepo(troll).byTree(categSlug, slug))
      } yield categ -> topic).value
      res ← data zmap {
        case (categ, topic) ⇒ (TopicRepo incViews topic) >>
          (env.postApi.paginator(topic, page, troll) map { (categ, topic, _).some })
      }
    } yield res

  def makeTopic(
    categ: Categ,
    data: DataForm.TopicData)(implicit ctx: Context): Fu[Topic] =
    TopicRepo.nextSlug(categ, data.name) flatMap { slug ⇒
      val topic = Topic.make(
        categId = categ.slug,
        slug = slug,
        name = data.name,
        troll = ctx.troll)
      val post = Post.make(
        topicId = topic.id,
        author = data.post.author,
        userId = ctx.me map (_.id),
        ip = ctx.isAnon option ctx.req.remoteAddress,
        troll = ctx.troll,
        text = data.post.text,
        number = 1,
        categId = categ.id)
      $insert(post) >>
        $insert(topic withPost post) >>
        $update(categ withTopic post) >>-
        (indexer ! InsertPost(post)) >>
        env.recent.invalidate inject topic
    }

  def paginator(categ: Categ, page: Int, troll: Boolean): Fu[Paginator[TopicView]] = Paginator(
    adapter = new Adapter[Topic](
      selector = TopicRepo(troll) byCategQuery categ,
      sort = Seq($sort.createdDesc)
    ) mapFuture { topic ⇒
      $find.byId[Post](topic lastPostId troll) map { post ⇒
        TopicView(categ, topic, post, env.postApi lastPageOf topic, troll)
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

  def toggleClose(categ: Categ, topic: Topic, mod: User): Funit =
    TopicRepo.close(topic.id, topic.open) >>
      modLog.toggleCloseTopic(mod, categ.name, topic.name, topic.open)

  def denormalize(topic: Topic): Funit = for {
    nbPosts ← PostRepo countByTopics List(topic)
    lastPost ← PostRepo lastByTopics List(topic)
    nbPostsTroll ← PostRepoTroll countByTopics List(topic)
    lastPostTroll ← PostRepoTroll lastByTopics List(topic)
    _ ← $update(topic.copy(
      nbPosts = nbPosts,
      lastPostId = lastPost zmap (_.id),
      updatedAt = lastPost.fold(topic.updatedAt)(_.createdAt),
      nbPostsTroll = nbPostsTroll,
      lastPostIdTroll = lastPostTroll zmap (_.id),
      updatedAtTroll = lastPostTroll.fold(topic.updatedAtTroll)(_.createdAt)
    ))
  } yield ()

  def denormalize: Funit = $find.all[Topic] flatMap { topics ⇒
    topics.map(denormalize).sequence
  } void
}
