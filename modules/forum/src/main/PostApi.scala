package lila.forum

import actorApi._
import lila.common.Bus
import lila.common.paginator._
import lila.db.dsl._
import lila.db.paginator._
import lila.hub.actorApi.timeline.{ ForumPost, Propagate }
import lila.mod.ModlogApi
import lila.security.{ Granter => MasterGranter }
import lila.user.{ User, UserContext }
import org.joda.time.DateTime

final class PostApi(
    categApi: => CategApi,
    topicApi: => TopicApi,
    categRepo: CategRepo,
    topicRepo: TopicRepo,
    postRepo: PostRepo,
    recent: Recent,
    indexer: lila.hub.actors.ForumSearch,
    maxPerPage: lila.common.config.MaxPerPage,
    modLog: ModlogApi,
    spam: lila.security.Spam,
    timeline: lila.hub.actors.Timeline,
    shutup: lila.hub.actors.Shutup,
    detectLanguage: lila.common.DetectLanguage,
    mentionNotifier: MentionNotifier
) {

  import BSONHandlers._

  def makePost(
    categ: Categ,
    topic: Topic,
    data: DataForm.PostData
  )(implicit ctx: UserContext): Fu[Post] =
    lastNumberOf(topic) zip detectLanguage(data.text) zip userIds(topic) flatMap {
      case ((number, lang), topicUserIds) =>
        val post = Post.make(
          topicId = topic.id,
          author = none,
          userId = ctx.me map (_.id),
          ip = ctx.req.remoteAddress.some,
          text = spam.replace(data.text),
          number = number + 1,
          lang = lang map (_.language),
          troll = ctx.troll,
          hidden = topic.hidden,
          categId = categ.id,
          modIcon = (~data.modIcon && ~ctx.me.map(MasterGranter(_.PublicMod))).option(true)
        )
        postRepo findDuplicate post flatMap {
          case Some(dup) => fuccess(dup)
          case _ =>
            postRepo.coll.insert.one(post) >>
              topicRepo.coll.update.one($id(topic.id), topic withPost post) >> {
                shouldHideOnPost(topic) ?? topicRepo.hide(topic.id, true)
              } >>
              categRepo.coll.update.one($id(categ.id), categ withTopic post) >>-
              (!categ.quiet ?? (indexer ! InsertPost(post))) >>-
              (!categ.quiet ?? recent.invalidate) >>-
              ctx.userId.?? { userId =>
                shutup ! {
                  if (post.isTeam) lila.hub.actorApi.shutup.RecordTeamForumMessage(userId, post.text)
                  else lila.hub.actorApi.shutup.RecordPublicForumMessage(userId, post.text)
                }
              } >>- {
                (ctx.userId ifFalse post.troll ifFalse categ.quiet) ?? { userId =>
                  timeline ! Propagate(ForumPost(userId, topic.id.some, topic.name, post.id)).|> { prop =>
                    prop toFollowersOf userId toUsers topicUserIds exceptUser userId
                  }
                }
                lila.mon.forum.post.create()
                mentionNotifier.notifyMentionedUsers(post, topic)
                Bus.publish(actorApi.CreatePost(post, topic), "forumPost")
              } inject post
        }
    }

  def editPost(postId: String, newText: String, user: User): Fu[Post] =
    get(postId) flatMap { post =>
      post.fold[Fu[Post]](fufail("Post no longer exists.")) {
        case (_, post) if !post.canBeEditedBy(user.id) =>
          fufail("You are not authorized to modify this post.")
        case (_, post) if !post.canStillBeEdited =>
          fufail("Post can no longer be edited")
        case (_, post) =>
          val newPost = post.editPost(DateTime.now, spam replace newText)
          postRepo.coll.update.one($id(post.id), newPost) inject newPost
      }
    }

  private val quickHideCategs = Set("lichess-feedback", "off-topic-discussion")

  private def shouldHideOnPost(topic: Topic) =
    topic.visibleOnHome && {
      (quickHideCategs(topic.categId) && topic.nbPosts == 1) || {
        topic.nbPosts == maxPerPage.value ||
          topic.createdAt.isBefore(DateTime.now minusDays 5)
      }
    }

  def urlData(postId: String, troll: Boolean): Fu[Option[PostUrlData]] = get(postId) flatMap {
    case Some((topic, post)) if (!troll && post.troll) => fuccess(none[PostUrlData])
    case Some((topic, post)) => postRepo.withTroll(troll).countBeforeNumber(topic.id, post.number) map { nb =>
      val page = nb / maxPerPage.value + 1
      PostUrlData(topic.categId, topic.slug, page, post.number).some
    }
    case _ => fuccess(none)
  }

  def get(postId: String): Fu[Option[(Topic, Post)]] = {
    for {
      post <- optionT(postRepo.coll.byId[Post](postId))
      topic <- optionT(topicRepo.coll.byId[Topic](post.topicId))
    } yield topic -> post
  } run

  def views(posts: List[Post]): Fu[List[PostView]] = for {
    topics <- topicRepo.coll.byIds[Topic](posts.map(_.topicId).distinct)
    categs <- categRepo.coll.byIds[Categ](topics.map(_.categId).distinct)
  } yield posts map { post =>
    for {
      topic <- topics find (_.id == post.topicId)
      categ <- categs find (_.slug == topic.categId)
    } yield PostView(post, topic, categ, lastPageOf(topic))
  } flatten

  def viewsFromIds(postIds: Seq[Post.ID]): Fu[List[PostView]] =
    postRepo.coll.byOrderedIds[Post, Post.ID](postIds)(_.id) flatMap views

  def view(post: Post): Fu[Option[PostView]] =
    views(List(post)) map (_.headOption)

  def liteViews(posts: Seq[Post]): Fu[Seq[PostLiteView]] =
    for {
      topics <- topicRepo.coll.byIds[Topic](posts.map(_.topicId).distinct)
    } yield posts flatMap { post =>
      topics find (_.id == post.topicId) map { topic =>
        PostLiteView(post, topic)
      }
    }
  def liteViewsByIds(postIds: Seq[Post.ID]): Fu[Seq[PostLiteView]] =
    postRepo.byIds(postIds) flatMap liteViews

  def liteView(post: Post): Fu[Option[PostLiteView]] =
    liteViews(List(post)) map (_.headOption)

  def miniPosts(posts: List[Post]): Fu[List[MiniForumPost]] = for {
    topics <- topicRepo.coll.byIds[Topic](posts.map(_.topicId).distinct)
  } yield posts flatMap { post =>
    topics find (_.id == post.topicId) map { topic =>
      MiniForumPost(
        isTeam = post.isTeam,
        postId = post.id,
        topicName = topic.name,
        userId = post.userId,
        text = post.text take 200,
        createdAt = post.createdAt
      )
    }
  }

  def lastNumberOf(topic: Topic): Fu[Int] =
    postRepo lastByTopic topic map { _ ?? (_.number) }

  def lastPageOf(topic: Topic) =
    math.ceil(topic.nbPosts / maxPerPage.value.toFloat).toInt

  def paginator(topic: Topic, page: Int, troll: Boolean): Fu[Paginator[Post]] = Paginator(
    new Adapter(
      collection = postRepo.coll,
      selector = postRepo.withTroll(troll) selectTopic topic.id,
      projection = none,
      sort = postRepo.sortQuery
    ),
    currentPage = page,
    maxPerPage = maxPerPage
  )

  def delete(categSlug: String, postId: String, mod: User): Funit = (for {
    post <- optionT(postRepo.withTroll(true).byCategAndId(categSlug, postId))
    view <- optionT(view(post))
    _ <- optionT(for {
      first <- postRepo.isFirstPost(view.topic.id, view.post.id)
      _ <- if (first) topicApi.delete(view.categ, view.topic)
      else postRepo.coll.delete.one(view.post) >>
        (topicApi denormalize view.topic) >>
        (categApi denormalize view.categ) >>-
        recent.invalidate >>-
        (indexer ! RemovePost(post.id))
      _ <- MasterGranter(_.ModerateForum)(mod) ?? modLog.deletePost(mod.id, post.userId, post.author, post.ip,
        text = "%s / %s / %s".format(view.categ.name, view.topic.name, post.text))
    } yield true.some)
  } yield ()).run.void

  def nbByUser(userId: String) = postRepo.coll.countSel($doc("userId" -> userId))

  def userIds(topic: Topic) = postRepo userIdsByTopicId topic.id

  def userIds(topicId: String) = postRepo userIdsByTopicId topicId

  def erase(user: User) = postRepo.coll.update.one(
    $doc("userId" -> user.id),
    $unset("userId", "editHistory", "lang", "ip") ++
      $set("text" -> "", "erasedAt" -> DateTime.now),
    multi = true
  )
}
