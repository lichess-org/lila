package lila.forum

import actorApi._
import akka.actor.ActorSelection
import org.joda.time.DateTime
import play.api.libs.json._

import lila.common.paginator._
import lila.db.api._
import lila.db.Implicits._
import lila.db.paginator._
import lila.hub.actorApi.timeline.{ Propagate, ForumPost }
import lila.mod.ModlogApi
import lila.security.{ Granter => MasterGranter }
import lila.user.{ User, UserContext }
import tube._

final class PostApi(
    env: Env,
    indexer: ActorSelection,
    maxPerPage: Int,
    modLog: ModlogApi,
    shutup: ActorSelection,
    timeline: ActorSelection,
    detectLanguage: lila.common.DetectLanguage) {

  def makePost(
    categ: Categ,
    topic: Topic,
    data: DataForm.PostData)(implicit ctx: UserContext): Fu[Post] =
    lastNumberOf(topic) zip detectLanguage(data.text) zip userIds(topic) flatMap {
      case ((number, lang), topicUserIds) =>
        val post = Post.make(
          topicId = topic.id,
          author = data.author,
          userId = ctx.me map (_.id),
          ip = ctx.req.remoteAddress.some,
          text = lila.security.Spam.replace(data.text),
          number = number + 1,
          lang = lang map (_.language),
          troll = ctx.troll,
          hidden = topic.hidden,
          categId = categ.id)
        PostRepo findDuplicate post flatMap {
          case Some(dup) => fuccess(dup)
          case _ =>
            $insert(post) >>
              $update(topic withPost post) >> {
                shouldHideOnPost(topic) ?? TopicRepo.hide(topic.id, true)
              } >>
              $update(categ withTopic post) >>-
              (indexer ! InsertPost(post)) >>
              (env.recent.invalidate inject post) >>-
              ctx.userId.?? { userId =>
                shutup ! post.isTeam.fold(
                  lila.hub.actorApi.shutup.RecordTeamForumMessage(userId, post.text),
                  lila.hub.actorApi.shutup.RecordPublicForumMessage(userId, post.text))
              } >>- {
                (ctx.userId ifFalse post.troll) ?? { userId =>
                  timeline ! Propagate(ForumPost(userId, topic.id.some, topic.name, post.id)).|>(prop =>
                    post.isStaff.fold(
                      prop toStaffFriendsOf userId,
                      prop toFollowersOf userId toUsers topicUserIds exceptUser userId
                    )
                  )
                }
                lila.mon.forum.post.create()
              } inject post
        }
    }

  private val quickHideCategs = Set("lichess-feedback", "off-topic-discussion")

  private def shouldHideOnPost(topic: Topic) =
    topic.visibleOnHome && {
      (quickHideCategs(topic.categId) && topic.nbPosts == 1) || {
        topic.nbPosts == maxPerPage ||
          topic.createdAt.isBefore(DateTime.now minusDays 5)
      }
    }

  def urlData(postId: String, troll: Boolean): Fu[Option[PostUrlData]] = get(postId) flatMap {
    case Some((topic, post)) if (!troll && post.troll) => fuccess(none[PostUrlData])
    case Some((topic, post)) => PostRepo(troll).countBeforeNumber(topic.id, post.number) map { nb =>
      val page = nb / maxPerPage + 1
      PostUrlData(topic.categId, topic.slug, page, post.number).some
    }
    case _ => fuccess(none)
  }

  def get(postId: String): Fu[Option[(Topic, Post)]] = for {
    post ← optionT($find.byId[Post](postId))
    topic ← optionT($find.byId[Topic](post.topicId))
  } yield topic -> post

  def views(posts: List[Post]): Fu[List[PostView]] = for {
    topics ← $find.byIds[Topic](posts.map(_.topicId).distinct)
    categs ← $find.byIds[Categ](topics.map(_.categId).distinct)
  } yield posts map { post =>
    for {
      topic ← topics find (_.id == post.topicId)
      categ ← categs find (_.slug == topic.categId)
    } yield PostView(post, topic, categ, lastPageOf(topic))
  } flatten

  def viewsFromIds(postIds: Seq[String]): Fu[List[PostView]] =
    $find.byOrderedIds[Post](postIds) flatMap views

  def view(post: Post): Fu[Option[PostView]] =
    views(List(post)) map (_.headOption)

  def liteViews(posts: List[Post]): Fu[List[PostLiteView]] = for {
    topics ← $find.byIds[Topic](posts.map(_.topicId).distinct)
  } yield posts flatMap { post =>
    topics find (_.id == post.topicId) map { topic =>
      PostLiteView(post, topic)
    }
  }

  def liteView(post: Post): Fu[Option[PostLiteView]] =
    liteViews(List(post)) map (_.headOption)

  def miniPosts(posts: List[Post]): Fu[List[MiniForumPost]] = for {
    topics ← $find.byIds[Topic](posts.map(_.topicId).distinct)
  } yield posts flatMap { post =>
    topics find (_.id == post.topicId) map { topic =>
      MiniForumPost(
        isTeam = post.isTeam,
        postId = post.id,
        topicName = topic.name,
        userId = post.userId,
        text = post.text take 200,
        createdAt = post.createdAt)
    }
  }

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
  } yield ()).run.void

  def nbByUser(userId: String) = $count[Post](Json.obj("userId" -> userId))

  def userIds(topic: Topic) = PostRepo userIdsByTopicId topic.id
}
