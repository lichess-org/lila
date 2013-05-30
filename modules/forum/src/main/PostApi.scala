package lila.forum

import play.api.libs.json._
import scalaz.{ OptionT, OptionTs }

import actorApi._
import lila.common.paginator._
import lila.db.api._
import lila.db.Implicits._
import lila.db.paginator._
import lila.hub.actorApi.timeline.{ Propagate, ForumPost }
import lila.hub.ActorLazyRef
import lila.mod.ModlogApi
import lila.security.{ Granter ⇒ MasterGranter }
import lila.user.{ User, Context }
import tube._

final class PostApi(
    env: Env,
    indexer: ActorLazyRef,
    maxPerPage: Int,
    modLog: ModlogApi,
    timeline: ActorLazyRef) extends OptionTs {

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
        troll = ctx.troll,
        categId = categ.id)
      $insert(post) >>
        $update(topic withPost post) >>
        $update(categ withTopic post) >>-
        (indexer ! InsertPost(post)) >>
        (env.recent.invalidate inject post) >>-
        ((ctx.userId ifFalse post.troll) ?? { userId ⇒
          timeline ! Propagate(ForumPost(userId, topic.name, post.id)).toFriendsOf(userId)
        }) inject post
    }

  def urlData(postId: String, troll: Boolean): Fu[Option[PostUrlData]] = get(postId) flatMap {
    case Some((topic, post)) if (!troll && post.troll) ⇒ fuccess(none[PostUrlData])
    case Some((topic, post)) ⇒ PostRepo(troll).countBeforeNumber(topic.id, post.number) map { nb ⇒
      val page = nb / maxPerPage + 1
      PostUrlData(topic.categId, topic.slug, page, post.number).some
    }
    case _ ⇒ fuccess(none)
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
    PostRepo lastByTopics List(topic) map { _ ?? (_.number) }

  def lastPageOf(topic: Topic) =
    math.ceil(topic.nbPosts / maxPerPage.toFloat).toInt

  def paginator(topic: Topic, page: Int, troll: Boolean): Fu[Paginator[Post]] = Paginator(
    new Adapter(
      selector = PostRepo(troll) selectTopic topic,
      sort = PostRepo.sortQuery :: Nil),
    currentPage = page,
    maxPerPage = maxPerPage)

  def delete(categSlug: String, postId: String, mod: User): Funit = (for {
    post ← optionT(PostRepo(true).byCategAndId(categSlug, postId))
    view ← optionT(view(post))
    _ ← optionT(for {
      first ← PostRepo.isFirstPost(view.topic.id, view.post.id)
      _ ← first.fold(
        env.topicApi.delete(view.categ, view.topic),
        $remove[Post](view.post) >>
          (env.topicApi denormalize view.topic) >>
          (env.categApi denormalize view.categ) >>
          env.recent.invalidate >>-
          (indexer ! RemovePost(post)))
      _ ← MasterGranter(_.ModerateForum)(mod) ?? modLog.deletePost(mod, post.userId, post.author, post.ip,
        text = "%s / %s / %s".format(view.categ.name, view.topic.name, post.text))
    } yield true.some)
  } yield ()).value.void

  def nbByUser(userId: String) = $count[Post](Json.obj("userId" -> userId))
}
