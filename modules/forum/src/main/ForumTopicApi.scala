package lila.forum

import scala.concurrent.duration.*

import lila.common.Bus
import lila.common.paginator.*
import lila.common.String.noShouting
import lila.db.dsl.{ *, given }
import lila.hub.actorApi.timeline.{ ForumPost as TimelinePost, Propagate }
import lila.memo.CacheApi
import lila.security.{ Granter as MasterGranter }
import lila.user.{ Holder, User }

final private class ForumTopicApi(
    postRepo: ForumPostRepo,
    topicRepo: ForumTopicRepo,
    categRepo: ForumCategRepo,
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
)(using ec: scala.concurrent.ExecutionContext):

  import BSONHandlers.given

  def show(
      categId: ForumCategId,
      slug: String,
      page: Int,
      forUser: Option[User]
  )(implicit
      netDomain: lila.common.config.NetDomain
  ): Fu[Option[(ForumCateg, ForumTopic, Paginator[ForumPost.WithFrag])]] =
    for {
      data <- categRepo byId categId flatMap {
        _ ?? { categ =>
          topicRepo.forUser(forUser).byTree(categId, slug) dmap {
            _ map (categ -> _)
          }
        }
      }
      res <- data ?? { case (categ, topic) =>
        lila.mon.forum.topic.view.increment()
        paginator.topicPosts(topic, page, forUser) map { (categ, topic, _).some }
      }
    } yield res

  object findDuplicate:
    private val cache =
      cacheApi.notLoadingSync[(UserId, String), ForumTopicId](64, "forum.topic.duplicate") {
        _.expireAfterWrite(1 hour).build()
      }
    def apply(topic: ForumTopic): Fu[Option[ForumTopic]] = topic.userId ?? { uid =>
      val key = (uid, topic.name)
      cache.getIfPresent(key) ?? { topicRepo.coll.byId[ForumTopic](_) } orElse {
        cache.put(key, topic.id)
        fuccess(none)
      }
    }

  def makeTopic(
      categ: ForumCateg,
      data: ForumForm.TopicData,
      me: User
  ): Fu[ForumTopic] =
    topicRepo.nextSlug(categ, data.name) zip detectLanguage(data.post.text) flatMap { case (slug, lang) =>
      val topic = ForumTopic.make(
        categId = categ.slug,
        slug = slug,
        name = noShouting(data.name),
        userId = me.id,
        troll = me.marks.troll
      )
      val post = ForumPost.make(
        topicId = topic.id,
        userId = me.id.some,
        troll = me.marks.troll,
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
                timeline ! Propagate(TimelinePost(me.id, topic.id, topic.name, post.id))
                  .toFollowersOf(me.id)
                  .withTeam(categ.team)
              lila.mon.forum.post.create.increment()
              mentionNotifier.notifyMentionedUsers(post, topic)
              Bus.publish(CreatePost(post), "forumPost")
            } inject topic
      }
    }

  def makeUblogDiscuss(
      slug: String,
      name: String,
      url: String,
      ublogId: String,
      authorId: UserId
  ): Funit =
    categRepo.byId(ForumCateg.ublogId) flatMap {
      _ ?? { categ =>
        val topic = ForumTopic.make(
          categId = categ.slug,
          slug = slug,
          name = name,
          troll = false,
          userId = authorId,
          ublogId = ublogId.some
        )
        val post = ForumPost.make(
          topicId = topic.id,
          userId = authorId.some,
          troll = false,
          text = s"Comments on $url",
          lang = none,
          number = 1,
          categId = categ.id
        )
        makeNewTopic(categ, topic, post)
      }
    }

  def makeBlogDiscuss(categ: ForumCateg, slug: String, name: String, url: String) =
    val topic = ForumTopic.make(
      categId = categ.slug,
      slug = slug,
      name = name,
      troll = false,
      userId = User.lichessId
    )
    val post = ForumPost.make(
      topicId = topic.id,
      userId = User.lichessId.some,
      troll = false,
      text = s"Comments on $url",
      lang = none,
      number = 1,
      categId = categ.id,
      modIcon = true.some
    )
    makeNewTopic(categ, topic, post)

  private def makeNewTopic(categ: ForumCateg, topic: ForumTopic, post: ForumPost) =
    postRepo.coll.insert.one(post) >>
      topicRepo.coll.insert.one(topic withPost post) >>
      categRepo.coll.update.one($id(categ.id), categ.withPost(topic, post)) >>-
      (indexer ! InsertPost(post)) >>-
      Bus.publish(CreatePost(post), "forumPost") void

  def getSticky(categ: ForumCateg, forUser: Option[User]): Fu[List[TopicView]] =
    topicRepo.stickyByCateg(categ) flatMap { topics =>
      topics.map { topic =>
        postRepo.coll.byId[ForumPost](topic lastPostId forUser) map { post =>
          TopicView(categ, topic, post, topic lastPage config.postMaxPerPage, forUser)
        }
      }.sequenceFu
    }

  def toggleClose(categ: ForumCateg, topic: ForumTopic, mod: Holder): Funit =
    topicRepo.close(topic.id, topic.open) >> {
      (MasterGranter.is(_.ModerateForum)(mod) || topic.isAuthor(mod.user)) ?? {
        modLog.toggleCloseTopic(mod.id into ModId, categ.id, topic.slug, topic.open)
      }
    }

  def toggleSticky(categ: ForumCateg, topic: ForumTopic, mod: Holder): Funit =
    topicRepo.sticky(topic.id, !topic.isSticky) >> {
      MasterGranter.is(_.ModerateForum)(mod) ??
        modLog.toggleStickyTopic(mod.id into ModId, categ.id, topic.slug, !topic.isSticky)
    }

  def denormalize(topic: ForumTopic): Funit =
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
              lastPostId = lastPost.fold(topic.lastPostId)(_.id),
              updatedAt = lastPost.fold(topic.updatedAt)(_.createdAt),
              nbPostsTroll = nbPostsTroll,
              lastPostIdTroll = lastPostTroll.fold(topic.lastPostIdTroll)(_.id),
              updatedAtTroll = lastPostTroll.fold(topic.updatedAtTroll)(_.createdAt)
            )
          )
          .void
    } yield ()
