package lila.forum

import actorApi._
import akka.actor.ActorSelection

import lila.common.paginator._
import lila.db.dsl._
import lila.db.paginator._
import lila.hub.actorApi.timeline.{ Propagate, ForumPost }
import lila.security.{ Granter => MasterGranter }
import lila.user.{ User, UserContext }

private[forum] final class TopicApi(
    env: Env,
    indexer: ActorSelection,
    maxPerPage: lila.common.MaxPerPage,
    modLog: lila.mod.ModlogApi,
    spam: lila.security.Spam,
    shutup: ActorSelection,
    timeline: ActorSelection,
    detectLanguage: lila.common.DetectLanguage,
    mentionNotifier: MentionNotifier,
    bus: lila.common.Bus
) {

  import BSONHandlers._

  def show(categSlug: String, slug: String, page: Int, troll: Boolean): Fu[Option[(Categ, Topic, Paginator[Post])]] =
    for {
      data ← (for {
        categ ← optionT(CategRepo bySlug categSlug)
        topic ← optionT(TopicRepo(troll).byTree(categSlug, slug))
      } yield categ -> topic).run
      res ← data ?? {
        case (categ, topic) =>
          lila.mon.forum.topic.view()
          TopicRepo incViews topic
          env.postApi.paginator(topic, page, troll) map { (categ, topic, _).some }
      }
    } yield res

  def makeTopic(
    categ: Categ,
    data: DataForm.TopicData
  )(implicit ctx: UserContext): Fu[Topic] =
    TopicRepo.nextSlug(categ, data.name) zip detectLanguage(data.post.text) flatMap {
      case (slug, lang) =>
        val topic = Topic.make(
          categId = categ.slug,
          slug = slug,
          name = data.name,
          troll = ctx.troll,
          hidden = categ.quiet || data.looksLikeVenting
        )
        val post = Post.make(
          topicId = topic.id,
          author = none,
          userId = ctx.me map (_.id),
          ip = ctx.isAnon option ctx.req.remoteAddress,
          troll = ctx.troll,
          hidden = topic.hidden,
          text = spam.replace(data.post.text),
          lang = lang map (_.language),
          number = 1,
          categId = categ.id,
          modIcon = (~data.post.modIcon && ~ctx.me.map(MasterGranter(_.PublicMod))).option(true)
        )
        env.postColl.insert(post) >>
          env.topicColl.insert(topic withPost post) >>
          env.categColl.update($id(categ.id), categ withTopic post) >>-
          (!categ.quiet ?? (indexer ! InsertPost(post))) >>-
          (!categ.quiet ?? env.recent.invalidate) >>-
          ctx.userId.?? { userId =>
            val text = s"${topic.name} ${post.text}"
            shutup ! {
              if (post.isTeam) lila.hub.actorApi.shutup.RecordTeamForumMessage(userId, text)
              else lila.hub.actorApi.shutup.RecordPublicForumMessage(userId, text)
            }
          } >>- {
            (ctx.userId ifFalse post.troll ifFalse categ.quiet) ?? { userId =>
              timeline ! Propagate(ForumPost(userId, topic.id.some, topic.name, post.id)).toFollowersOf(userId)
            }
            lila.mon.forum.post.create()
          } >>- {
            mentionNotifier.notifyMentionedUsers(post, topic)
            bus.publish(actorApi.CreatePost(post, topic), 'forumPost)
          } inject topic
    }

  def makeBlogDiscuss(categ: Categ, slug: String, name: String, url: String): Funit = {
    val topic = Topic.make(
      categId = categ.slug,
      slug = slug,
      name = name,
      troll = false,
      hidden = false
    )
    val post = Post.make(
      topicId = topic.id,
      author = none,
      userId = lila.user.User.lichessId.some,
      ip = none,
      troll = false,
      hidden = false,
      text = s"Comments on $url",
      lang = none,
      number = 1,
      categId = categ.id,
      modIcon = true.some
    )
    env.postColl.insert(post) >>
      env.topicColl.insert(topic withPost post) >>
      env.categColl.update($id(categ.id), categ withTopic post) >>-
      (indexer ! InsertPost(post)) >>-
      env.recent.invalidate >>-
      bus.publish(actorApi.CreatePost(post, topic), 'forumPost) void
  }

  def paginator(categ: Categ, page: Int, troll: Boolean): Fu[Paginator[TopicView]] = {
    val adapter = new Adapter[Topic](
      collection = env.topicColl,
      selector = TopicRepo(troll) byCategNotStickyQuery categ,
      projection = $empty,
      sort = $sort.updatedDesc
    ) mapFutureList { topics =>
      env.postColl.optionsByOrderedIds[Post, String](topics.map(_ lastPostId troll))(_.id) map { posts =>
        topics zip posts map {
          case topic ~ post => TopicView(categ, topic, post, env.postApi lastPageOf topic, troll)
        }
      }
    }
    val cachedAdapter =
      if (categ.isTeam) adapter
      else new CachedAdapter(adapter, nbResults = fuccess(1000))
    Paginator(
      adapter = cachedAdapter,
      currentPage = page,
      maxPerPage = maxPerPage
    )
  }

  def getSticky(categ: Categ, troll: Boolean): Fu[List[TopicView]] =
    TopicRepo.stickyByCateg(categ) flatMap { topics =>
      scala.concurrent.Future.sequence(topics map {
        topic =>
          {
            env.postColl.byId[Post](topic lastPostId troll) map { post =>
              TopicView(categ, topic, post, env.postApi lastPageOf topic, troll)
            }
          }
      })
    }

  def delete(categ: Categ, topic: Topic): Funit =
    PostRepo.idsByTopicId(topic.id) flatMap { postIds =>
      (PostRepo removeByTopic topic.id zip env.topicColl.remove($id(topic.id))) >>
        (env.categApi denormalize categ) >>-
        (indexer ! RemovePosts(postIds)) >>-
        env.recent.invalidate
    }

  def toggleClose(categ: Categ, topic: Topic, mod: User): Funit =
    TopicRepo.close(topic.id, topic.open) >> {
      MasterGranter(_.ModerateForum)(mod) ??
        modLog.toggleCloseTopic(mod.id, categ.name, topic.name, topic.open)
    }

  def toggleHide(categ: Categ, topic: Topic, mod: User): Funit =
    TopicRepo.hide(topic.id, topic.visibleOnHome) >> {
      MasterGranter(_.ModerateForum)(mod) ?? {
        PostRepo.hideByTopic(topic.id, topic.visibleOnHome) >>
          modLog.toggleHideTopic(mod.id, categ.name, topic.name, topic.visibleOnHome)
      } >>- env.recent.invalidate
    }

  def toggleSticky(categ: Categ, topic: Topic, mod: User): Funit =
    TopicRepo.sticky(topic.id, !topic.isSticky) >> {
      MasterGranter(_.ModerateForum)(mod) ??
        modLog.toggleStickyTopic(mod.id, categ.name, topic.name, topic.isSticky)
    }

  def denormalize(topic: Topic): Funit = for {
    nbPosts ← PostRepo countByTopic topic
    lastPost ← PostRepo lastByTopic topic
    nbPostsTroll ← PostRepoTroll countByTopic topic
    lastPostTroll ← PostRepoTroll lastByTopic topic
    _ ← env.topicColl.update($id(topic.id), topic.copy(
      nbPosts = nbPosts,
      lastPostId = lastPost ?? (_.id),
      updatedAt = lastPost.fold(topic.updatedAt)(_.createdAt),
      nbPostsTroll = nbPostsTroll,
      lastPostIdTroll = lastPostTroll ?? (_.id),
      updatedAtTroll = lastPostTroll.fold(topic.updatedAtTroll)(_.createdAt)
    )).void
  } yield ()
}
