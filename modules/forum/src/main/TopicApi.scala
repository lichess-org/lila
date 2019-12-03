package lila.forum

import actorApi._

import lila.common.Bus
import lila.common.paginator._
import lila.db.dsl._
import lila.db.paginator._
import lila.hub.actorApi.timeline.{ Propagate, ForumPost }
import lila.security.{ Granter => MasterGranter }
import lila.user.{ User, UserContext }

private[forum] final class TopicApi(
    postApi: => PostApi,
    categApi: => CategApi,
    categRepo: CategRepo,
    topicRepo: TopicRepo,
    postRepo: PostRepo,
    recent: Recent,
    indexer: lila.hub.actors.ForumSearch,
    maxPerPage: lila.common.config.MaxPerPage,
    modLog: lila.mod.ModlogApi,
    spam: lila.security.Spam,
    timeline: lila.hub.actors.Timeline,
    shutup: lila.hub.actors.Shutup,
    detectLanguage: lila.common.DetectLanguage,
    mentionNotifier: MentionNotifier
) {

  import BSONHandlers._

  def show(categSlug: String, slug: String, page: Int, troll: Boolean): Fu[Option[(Categ, Topic, Paginator[Post])]] =
    for {
      data <- (for {
        categ <- optionT(categRepo bySlug categSlug)
        topic <- optionT(topicRepo.withTroll(troll).byTree(categSlug, slug))
      } yield categ -> topic).run
      res <- data ?? {
        case (categ, topic) =>
          lila.mon.forum.topic.view()
          topicRepo incViews topic
          postApi.paginator(topic, page, troll) map { (categ, topic, _).some }
      }
    } yield res

  def makeTopic(
    categ: Categ,
    data: DataForm.TopicData
  )(implicit ctx: UserContext): Fu[Topic] =
    topicRepo.nextSlug(categ, data.name) zip detectLanguage(data.post.text) flatMap {
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
        postRepo.coll.insert.one(post) >>
          topicRepo.coll.insert.one(topic withPost post) >>
          categRepo.coll.update.one($id(categ.id), categ withTopic post) >>-
          (!categ.quiet ?? (indexer ! InsertPost(post))) >>-
          (!categ.quiet ?? recent.invalidate) >>-
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
            Bus.publish(actorApi.CreatePost(post, topic), "forumPost")
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
    postRepo.coll.insert.one(post) >>
      topicRepo.coll.insert.one(topic withPost post) >>
      categRepo.coll.update.one($id(categ.id), categ withTopic post) >>-
      (indexer ! InsertPost(post)) >>-
      recent.invalidate >>-
      Bus.publish(actorApi.CreatePost(post, topic), "forumPost") void
  }

  def paginator(categ: Categ, page: Int, troll: Boolean): Fu[Paginator[TopicView]] = {
    val adapter = new Adapter[Topic](
      collection = topicRepo.coll,
      selector = topicRepo.withTroll(troll) byCategNotStickyQuery categ,
      projection = none,
      sort = $sort.updatedDesc
    ) mapFutureList { topics =>
      postRepo.coll.optionsByOrderedIds[Post, String](topics.map(_ lastPostId troll))(_.id) map { posts =>
        topics zip posts map {
          case topic ~ post => TopicView(categ, topic, post, postApi lastPageOf topic, troll)
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
    topicRepo.stickyByCateg(categ) flatMap { topics =>
      topics.map { topic =>
        postRepo.coll.byId[Post](topic lastPostId troll) map { post =>
          TopicView(categ, topic, post, postApi lastPageOf topic, troll)
        }
      }.sequenceFu
    }

  def delete(categ: Categ, topic: Topic): Funit =
    postRepo.idsByTopicId(topic.id) flatMap { postIds =>
      (postRepo removeByTopic topic.id zip topicRepo.coll.delete.one($id(topic.id))) >>
        (categApi denormalize categ) >>-
        (indexer ! RemovePosts(postIds)) >>-
        recent.invalidate
    }

  def toggleClose(categ: Categ, topic: Topic, mod: User): Funit =
    topicRepo.close(topic.id, topic.open) >> {
      MasterGranter(_.ModerateForum)(mod) ??
        modLog.toggleCloseTopic(mod.id, categ.name, topic.name, topic.open)
    }

  def toggleHide(categ: Categ, topic: Topic, mod: User): Funit =
    topicRepo.hide(topic.id, topic.visibleOnHome) >> {
      MasterGranter(_.ModerateForum)(mod) ?? {
        postRepo.hideByTopic(topic.id, topic.visibleOnHome) >>
          modLog.toggleHideTopic(mod.id, categ.name, topic.name, topic.visibleOnHome)
      } >>- recent.invalidate
    }

  def toggleSticky(categ: Categ, topic: Topic, mod: User): Funit =
    topicRepo.sticky(topic.id, !topic.isSticky) >> {
      MasterGranter(_.ModerateForum)(mod) ??
        modLog.toggleStickyTopic(mod.id, categ.name, topic.name, topic.isSticky)
    }

  def denormalize(topic: Topic): Funit = for {
    nbPosts <- postRepo countByTopic topic
    lastPost <- postRepo lastByTopic topic
    nbPostsTroll <- postRepo withTroll true countByTopic topic
    lastPostTroll <- postRepo withTroll true lastByTopic topic
    _ <- topicRepo.coll.update.one($id(topic.id), topic.copy(
      nbPosts = nbPosts,
      lastPostId = lastPost ?? (_.id),
      updatedAt = lastPost.fold(topic.updatedAt)(_.createdAt),
      nbPostsTroll = nbPostsTroll,
      lastPostIdTroll = lastPostTroll ?? (_.id),
      updatedAtTroll = lastPostTroll.fold(topic.updatedAtTroll)(_.createdAt)
    )).void
  } yield ()
}
