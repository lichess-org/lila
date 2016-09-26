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
    maxPerPage: Int,
    modLog: lila.mod.ModlogApi,
    shutup: ActorSelection,
    timeline: ActorSelection,
    detectLanguage: lila.common.DetectLanguage,
    mentionNotifier: MentionNotifier) {

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
    data: DataForm.TopicData)(implicit ctx: UserContext): Fu[Topic] =
    TopicRepo.nextSlug(categ, data.name) zip detectLanguage(data.post.text) flatMap {
      case (slug, lang) =>
        val topic = Topic.make(
          categId = categ.slug,
          slug = slug,
          name = data.name,
          troll = ctx.troll,
          hidden = categ.quiet)
        val post = Post.make(
          topicId = topic.id,
          author = data.post.author,
          userId = ctx.me map (_.id),
          ip = ctx.isAnon option ctx.req.remoteAddress,
          troll = ctx.troll,
          hidden = topic.hidden,
          text = lila.security.Spam.replace(data.post.text),
          lang = lang map (_.language),
          number = 1,
          categId = categ.id)
        env.postColl.insert(post) >>
          env.topicColl.insert(topic withPost post) >>
          env.categColl.update($id(categ.id), categ withTopic post) >>-
          (!categ.quiet ?? (indexer ! InsertPost(post))) >>
          (!categ.quiet ?? env.recent.invalidate) >>-
          ctx.userId.?? { userId =>
            val text = topic.name + " " + post.text
            shutup ! post.isTeam.fold(
              lila.hub.actorApi.shutup.RecordTeamForumMessage(userId, text),
              lila.hub.actorApi.shutup.RecordPublicForumMessage(userId, text))
          } >>- {
            (ctx.userId ifFalse post.troll ifFalse categ.quiet) ?? { userId =>
              timeline ! Propagate(ForumPost(userId, topic.id.some, topic.name, post.id)).|>(prop =>
                post.isStaff.fold(prop toStaffFriendsOf userId, prop toFollowersOf userId)
              )
            }
            lila.mon.forum.post.create()
          } >>- mentionNotifier.notifyMentionedUsers(post, topic) inject topic
    }

  def paginator(categ: Categ, page: Int, troll: Boolean): Fu[Paginator[TopicView]] = {
    val adapter = new Adapter[Topic](
      collection = env.topicColl,
      selector = TopicRepo(troll) byCategQuery categ,
      projection = $empty,
      sort = $sort.updatedDesc
    ) mapFuture { topic =>
      env.postColl.byId[Post](topic lastPostId troll) map { post =>
        TopicView(categ, topic, post, env.postApi lastPageOf topic, troll)
      }
    }
    val cachedAdapter =
      if (categ.isTeam) adapter
      else new CachedAdapter(adapter, nbResults = fuccess(1000))
    Paginator(
      adapter = cachedAdapter,
      currentPage = page,
      maxPerPage = maxPerPage)
  }

  def delete(categ: Categ, topic: Topic): Funit =
    PostRepo.idsByTopicId(topic.id) flatMap { postIds =>
      (PostRepo removeByTopic topic.id zip env.topicColl.remove($id(topic.id))) >>
        (env.categApi denormalize categ) >>-
        (indexer ! RemovePosts(postIds)) >>
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
        PostRepo.hideByTopic(topic.id, topic.visibleOnHome) zip
          modLog.toggleHideTopic(mod.id, categ.name, topic.name, topic.visibleOnHome)
      } >> env.recent.invalidate
    }

  def denormalize(topic: Topic): Funit = for {
    nbPosts ← PostRepo countByTopics List(topic.id)
    lastPost ← PostRepo lastByTopics List(topic.id)
    nbPostsTroll ← PostRepoTroll countByTopics List(topic.id)
    lastPostTroll ← PostRepoTroll lastByTopics List(topic.id)
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
