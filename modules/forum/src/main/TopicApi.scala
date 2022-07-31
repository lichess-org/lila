package lila.forum

import actorApi._
import scala.concurrent.duration._

import lila.common.Bus
import lila.common.paginator._
import lila.common.String.noShouting
import lila.db.dsl._
import lila.hub.actorApi.timeline.{ ForumPost, Propagate }
import lila.memo.CacheApi
import lila.security.{ Granter => MasterGranter }
import lila.user.{ Holder, User }

final private[forum] class TopicApi(
    postRepo: PostRepo,
    topicRepo: TopicRepo,
    categRepo: CategRepo,
    mentionNotifier: MentionNotifier,
    paginator: ForumPaginator,
    indexer: lila.hub.actors.ForumSearch,
    config: ForumConfig,
    modLog: lila.mod.ModlogApi,
    spam: lila.security.Spam,
    promotion: lila.security.PromotionApi,
    timeline: lila.hub.actors.Timeline,
    shutup: lila.hub.actors.Shutup,
    detectLanguage: DetectLanguage,
    cacheApi: CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._

  def show(
      categSlug: String,
      slug: String,
      page: Int,
      forUser: Option[User]
  )(implicit netDomain: lila.common.config.NetDomain): Fu[Option[(Categ, Topic, Paginator[Post.WithFrag])]] =
    for {
      data <- categRepo bySlug categSlug flatMap {
        _ ?? { categ =>
          topicRepo.forUser(forUser).byTree(categSlug, slug) dmap {
            _ map (categ -> _)
          }
        }
      }
      res <- data ?? { case (categ, topic) =>
        lila.mon.forum.topic.view.increment()
        paginator.topicPosts(topic, page, forUser) map { (categ, topic, _).some }
      }
    } yield res

  object findDuplicate {
    private val cache = cacheApi.notLoadingSync[(User.ID, String), Topic.ID](64, "forum.topic.duplicate") {
      _.expireAfterWrite(1 hour).build()
    }
    def apply(topic: Topic): Fu[Option[Topic]] = {
      val key = (~topic.userId, topic.name)
      cache.getIfPresent(key) ?? topicRepo.coll.byId[Topic] orElse {
        cache.put(key, topic.id)
        fuccess(none)
      }
    }
  }

  def makeTopic(
      categ: Categ,
      data: ForumForm.TopicData,
      me: User
  ): Fu[Topic] =
    topicRepo.nextSlug(categ, data.name) zip detectLanguage(data.post.text) flatMap { case (slug, lang) =>
      val topic = Topic.make(
        categId = categ.slug,
        slug = slug,
        name = noShouting(data.name),
        userId = me.id,
        troll = me.marks.troll,
        hidden = categ.quiet || data.looksLikeVenting
      )
      val post = Post.make(
        topicId = topic.id,
        author = none,
        userId = me.id.some,
        troll = me.marks.troll,
        hidden = topic.hidden,
        text = spam.replace(data.post.text),
        lang = lang map (_.language),
        number = 1,
        categId = categ.id,
        modIcon = (~data.post.modIcon && MasterGranter(_.PublicMod)(me)).option(true)
      )
      findDuplicate(topic) flatMap {
        case Some(dup) => fuccess(dup)
        case None =>
          postRepo.coll.insert.one(post) >>
            topicRepo.coll.insert.one(topic withPost post) >>
            categRepo.coll.update.one($id(categ.id), categ.withPost(topic, post)) >>- {
              !categ.quiet ?? (indexer ! InsertPost(post))
              promotion.save(me, post.text)
              shutup ! {
                val text = s"${topic.name} ${post.text}"
                if (post.isTeam) lila.hub.actorApi.shutup.RecordTeamForumMessage(me.id, text)
                else lila.hub.actorApi.shutup.RecordPublicForumMessage(me.id, text)
              }
              if (!post.troll && !categ.quiet)
                timeline ! Propagate(ForumPost(me.id, topic.id.some, topic.name, post.id))
                  .toFollowersOf(me.id)
              lila.mon.forum.post.create.increment()
              mentionNotifier.notifyMentionedUsers(post, topic)
              Bus.publish(actorApi.CreatePost(post), "forumPost")
            } inject topic
      }
    }

  def makeUblogDiscuss(
      slug: String,
      name: String,
      url: String,
      ublogId: String,
      authorId: User.ID
  ): Funit =
    categRepo.bySlug(Categ.ublogSlug) flatMap {
      _ ?? { categ =>
        val topic = Topic.make(
          categId = categ.slug,
          slug = slug,
          name = name,
          troll = false,
          userId = authorId,
          hidden = false,
          ublogId = ublogId.some
        )
        val post = Post.make(
          topicId = topic.id,
          author = none,
          userId = authorId.some,
          troll = false,
          hidden = false,
          text = s"Comments on $url",
          lang = none,
          number = 1,
          categId = categ.id
        )
        makeNewTopic(categ, topic, post)
      }
    }

  def makeBlogDiscuss(categ: Categ, slug: String, name: String, url: String) = {
    val topic = Topic.make(
      categId = categ.slug,
      slug = slug,
      name = name,
      troll = false,
      userId = User.lichessId,
      hidden = false
    )
    val post = Post.make(
      topicId = topic.id,
      author = none,
      userId = User.lichessId.some,
      troll = false,
      hidden = false,
      text = s"Comments on $url",
      lang = none,
      number = 1,
      categId = categ.id,
      modIcon = true.some
    )
    makeNewTopic(categ, topic, post)
  }

  private def makeNewTopic(categ: Categ, topic: Topic, post: Post) = {
    postRepo.coll.insert.one(post) >>
      topicRepo.coll.insert.one(topic withPost post) >>
      categRepo.coll.update.one($id(categ.id), categ.withPost(topic, post)) >>-
      (indexer ! InsertPost(post)) >>-
      Bus.publish(actorApi.CreatePost(post), "forumPost") void
  }

  def getSticky(categ: Categ, forUser: Option[User]): Fu[List[TopicView]] =
    topicRepo.stickyByCateg(categ) flatMap { topics =>
      topics.map { topic =>
        postRepo.coll.byId[Post](topic lastPostId forUser) map { post =>
          TopicView(categ, topic, post, topic lastPage config.postMaxPerPage, forUser)
        }
      }.sequenceFu
    }

  def toggleClose(categ: Categ, topic: Topic, mod: Holder): Funit =
    topicRepo.close(topic.id, topic.open) >> {
      (MasterGranter.is(_.ModerateForum)(mod) || topic.isAuthor(mod.user)) ?? {
        modLog.toggleCloseTopic(mod.id, categ.id, topic.slug, topic.open)
      }
    }

  def toggleHide(categ: Categ, topic: Topic, mod: Holder): Funit =
    topicRepo.hide(topic.id, topic.visibleOnHome) >> {
      MasterGranter.is(_.ModerateForum)(mod) ?? {
        postRepo.hideByTopic(topic.id, topic.visibleOnHome) >>
          modLog.toggleHideTopic(mod.id, categ.id, topic.slug, topic.visibleOnHome)
      }
    }

  def toggleSticky(categ: Categ, topic: Topic, mod: Holder): Funit =
    topicRepo.sticky(topic.id, !topic.isSticky) >> {
      MasterGranter.is(_.ModerateForum)(mod) ??
        modLog.toggleStickyTopic(mod.id, categ.id, topic.slug, !topic.isSticky)
    }

  def denormalize(topic: Topic): Funit =
    for {
      nbPosts       <- postRepo countByTopic topic
      lastPost      <- postRepo lastByTopic topic
      nbPostsTroll  <- postRepo.unsafe countByTopic topic
      lastPostTroll <- postRepo.unsafe lastByTopic topic
      _ <-
        topicRepo.coll.update
          .one(
            $id(topic.id),
            topic.copy(
              nbPosts = nbPosts,
              lastPostId = lastPost ?? (_.id),
              updatedAt = lastPost.fold(topic.updatedAt)(_.createdAt),
              nbPostsTroll = nbPostsTroll,
              lastPostIdTroll = lastPostTroll ?? (_.id),
              updatedAtTroll = lastPostTroll.fold(topic.updatedAtTroll)(_.createdAt)
            )
          )
          .void
    } yield ()
}
