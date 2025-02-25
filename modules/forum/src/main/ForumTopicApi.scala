package lila.forum

import scalalib.paginator.*

import lila.common.Bus
import lila.common.String.noShouting
import lila.core.config.NetDomain
import lila.core.forum.BusForum.CreatePost
import lila.core.perm.Granter as MasterGranter
import lila.core.shutup.{ PublicSource, ShutupApi }
import lila.core.timeline.{ ForumPost as TimelinePost, Propagate }
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi

final private class ForumTopicApi(
    postRepo: ForumPostRepo,
    topicRepo: ForumTopicRepo,
    categRepo: ForumCategRepo,
    mentionNotifier: MentionNotifier,
    paginator: ForumPaginator,
    modLog: lila.core.mod.LogApi,
    config: ForumConfig,
    spam: lila.core.security.SpamApi,
    promotion: lila.core.security.PromotionApi,
    shutupApi: lila.core.shutup.ShutupApi,
    detectLanguage: DetectLanguage,
    cacheApi: CacheApi,
    relationApi: lila.core.relation.RelationApi
)(using Executor):

  import BSONHandlers.given

  def lastPage(topic: ForumTopic)(using NetDomain): Int =
    topic.nbPosts / config.postMaxPerPage.value + 1

  def showLastPage(categId: ForumCategId, slug: String)(using NetDomain)(using me: Option[Me]) =
    topicRepo
      .byTree(categId, slug)
      .flatMapz: topic =>
        show(categId, slug, topic.lastPage(config.postMaxPerPage))

  def show(categId: ForumCategId, slug: String, page: Int)(using
      NetDomain
  )(using me: Option[Me]): Fu[Option[(ForumCateg, ForumTopic, Paginator[ForumPost.WithFrag])]] =
    for
      data <- categRepo.byId(categId).flatMapz { categ =>
        topicRepo
          .forUser(me)
          .byTree(categId, slug)
          .dmap:
            _.map(categ -> _)
      }
      res <- data.so: (categ, topic) =>
        lila.mon.forum.topic.view.increment()
        paginator
          .topicPosts(topic, page)
          .flatMap: paginated =>
            val authors = paginated.currentPageResults.flatMap(_.post.userId)
            me.so(relationApi.filterBlocked(_, authors))
              .map: blockedAuthors =>
                (
                  categ,
                  topic,
                  paginated.mapResults: p =>
                    p.copy(hide = p.post.userId.so(blockedAuthors(_)))
                ).some
    yield res

  object findDuplicate:
    private val cache =
      cacheApi.notLoadingSync[(UserId, String), ForumTopicId](64, "forum.topic.duplicate"):
        _.expireAfterWrite(1.hour).build()
    def apply(topic: ForumTopic): Fu[Option[ForumTopic]] = topic.userId.so: uid =>
      val key = (uid, topic.name)
      cache
        .getIfPresent(key)
        .so { topicRepo.coll.byId[ForumTopic](_) }
        .orElse:
          cache.put(key, topic.id)
          fuccess(none)

  def makeTopic(
      categ: ForumCateg,
      data: ForumForm.TopicData
  )(using me: Me): Fu[ForumTopic] =
    topicRepo.nextSlug(categ, data.name).zip(detectLanguage(data.post.text)).flatMap { (slug, lang) =>
      val topic = ForumTopic.make(
        categId = categ.id,
        slug = slug,
        name = noShouting(data.name),
        userId = me,
        troll = me.marks.troll
      )
      val post = ForumPost.make(
        topicId = topic.id,
        userId = me.some,
        troll = me.marks.troll,
        text = spam.replace(data.post.text),
        lang = lang.map(_.language),
        number = 1,
        categId = categ.id,
        modIcon = (~data.post.modIcon && MasterGranter(_.PublicMod)).option(true)
      )
      findDuplicate(topic).flatMap:
        case Some(dup) => fuccess(dup)
        case None =>
          for
            _ <- topicRepo.coll.insert.one(topic.withPost(post))
            _ <- categRepo.coll.update.one($id(categ.id), categ.withPost(topic, post))
            _ <- postRepo.coll.insert.one(post)
          yield
            promotion.save(me, post.text)
            val text = s"${topic.name} ${post.text}"
            if post.isTeam then shutupApi.teamForumMessage(me, text)
            else shutupApi.publicText(me, text, PublicSource.Forum(post.id))
            if !post.troll && !categ.quiet then
              lila.common.Bus.pub:
                Propagate(TimelinePost(me, topic.id, topic.name, post.id))
                  .toFollowersOf(me)
                  .withTeam(categ.team)
            lila.mon.forum.post.create.increment()
            mentionNotifier.notifyMentionedUsers(post, topic)
            Bus.pub(CreatePost(post.mini))
            topic
    }

  def makeUblogDiscuss(
      slug: String,
      name: String,
      url: String,
      ublogId: UblogPostId,
      authorId: UserId
  ): Funit =
    categRepo.byId(ForumCateg.ublogId).flatMapz { categ =>
      val topic = ForumTopic.make(
        categId = categ.id,
        slug = slug,
        name = name,
        userId = authorId,
        ublogId = ublogId.some
      )
      makeNewTopic(
        categ,
        topic,
        ForumPost.make(
          topicId = topic.id,
          userId = authorId.some,
          text = s"Comments on $url",
          categId = categ.id
        )
      )
    }

  private def makeNewTopic(categ: ForumCateg, topic: ForumTopic, post: ForumPost) = for
    _ <- topicRepo.coll.insert.one(topic.withPost(post))
    _ <- categRepo.coll.update.one($id(categ.id), categ.withPost(topic, post))
    _ <- postRepo.coll.insert.one(post)
  yield Bus.pub(CreatePost(post.mini))

  def getSticky(categ: ForumCateg, forUser: Option[User]): Fu[List[TopicView]] =
    topicRepo.stickyByCateg(categ).flatMap { topics =>
      topics.sequentially: topic =>
        postRepo.coll.byId[ForumPost](topic.lastPostId(forUser)).map { post =>
          TopicView(categ, topic, post, topic.lastPage(config.postMaxPerPage), forUser)
        }
    }

  def toggleClose(categ: ForumCateg, topic: ForumTopic)(using me: Me): Funit =
    topicRepo.close(topic.id, topic.open) >> {
      (MasterGranter(_.ModerateForum) || topic.isAuthor(me.value)).so:
        modLog.toggleCloseTopic(categ.id, topic.slug, topic.open)
    }

  def toggleSticky(categ: ForumCateg, topic: ForumTopic)(using Me): Funit =
    topicRepo.sticky(topic.id, !topic.isSticky) >> {
      MasterGranter(_.ModerateForum).so(modLog.toggleStickyTopic(categ.id, topic.slug, !topic.isSticky))
    }

  def denormalize(topic: ForumTopic): Funit = for
    nbPosts       <- postRepo.countByTopic(topic)
    lastPost      <- postRepo.lastByTopic(topic)
    nbPostsTroll  <- postRepo.unsafe.countByTopic(topic)
    lastPostTroll <- postRepo.unsafe.lastByTopic(topic)
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
  yield ()

  def removeTopic(categId: ForumCategId, slug: String): Funit =
    topicRepo
      .byTree(categId, slug)
      .flatMap:
        case None => funit
        case Some(topic) =>
          for
            _        <- postRepo.removeByTopic(topic.id)
            _        <- topicRepo.remove(topic)
            categOpt <- categRepo.byId(categId)
          yield categOpt.foreach: cat =>
            for
              topics <- topicRepo.byCateg(cat)
              lastPostId      = topics.maxBy(_.updatedAt).lastPostId
              lastPostIdTroll = topics.maxBy(_.updatedAtTroll).lastPostIdTroll
              _ <- categRepo.coll.update
                .one($id(cat.id), cat.withoutTopic(topic, lastPostId, lastPostIdTroll))
            yield ()

  def relocate(fromTopic: ForumTopic, to: ForumCategId)(using Me): Fu[ForumTopic] =
    val topic = fromTopic.copy(
      slug = s"${fromTopic.slug}-${scalalib.ThreadLocalRandom.nextString(4)}"
    )
    for
      _ <- topicRepo.coll.update.one($id(topic.id), $set("categId" -> to, "slug" -> topic.slug))
      _ <- postRepo.coll.update.one($doc("topicId" -> topic.id), $set("categId" -> to))
    yield topic
