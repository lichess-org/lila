package lila.forum

import actorApi._
import akka.actor.ActorSelection

import lila.common.paginator._
import lila.db.api._
import lila.db.Implicits._
import lila.db.paginator._
import lila.hub.actorApi.timeline.{ Propagate, ForumPost }
import lila.security.{ Granter => MasterGranter }
import lila.user.{ User, UserContext }
import tube._

private[forum] final class TopicApi(
    env: Env,
    indexer: ActorSelection,
    maxPerPage: Int,
    modLog: lila.mod.ModlogApi,
    shutup: ActorSelection,
    timeline: ActorSelection,
    detectLanguage: lila.common.DetectLanguage) {

  def show(categSlug: String, slug: String, page: Int, troll: Boolean): Fu[Option[(Categ, Topic, Paginator[Post])]] =
    for {
      data ← (for {
        categ ← optionT(CategRepo bySlug categSlug)
        topic ← optionT(TopicRepo(troll).byTree(categSlug, slug))
      } yield categ -> topic).run
      res ← data ?? {
        case (categ, topic) =>
          lila.mon.forum.topic.view()
          (TopicRepo incViews topic) >>
            (env.postApi.paginator(topic, page, troll) map { (categ, topic, _).some })
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
          featured = true)
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
        $insert(post) >>
          $insert(topic withPost post) >>
          $update(categ withTopic post) >>-
          (indexer ! InsertPost(post)) >>
          env.recent.invalidate >>-
          ctx.userId.?? { userId =>
            val text = topic.name + " " + post.text
            shutup ! post.isTeam.fold(
              lila.hub.actorApi.shutup.RecordTeamForumMessage(userId, text),
              lila.hub.actorApi.shutup.RecordPublicForumMessage(userId, text))
          } >>- {
            (ctx.userId ifFalse post.troll) ?? { userId =>
              timeline ! Propagate(ForumPost(userId, topic.id.some, topic.name, post.id)).|>(prop =>
                post.isStaff.fold(prop toStaffFriendsOf userId, prop toFollowersOf userId)
              )
            }
            lila.mon.forum.post.create()
          } inject topic
    }

  def paginator(categ: Categ, page: Int, troll: Boolean): Fu[Paginator[TopicView]] = Paginator(
    adapter = new Adapter[Topic](
      selector = TopicRepo(troll) byCategQuery categ,
      sort = Seq($sort.updatedDesc)
    ) mapFuture { topic =>
      $find.byId[Post](topic lastPostId troll) map { post =>
        TopicView(categ, topic, post, env.postApi lastPageOf topic, troll)
      }
    },
    currentPage = page,
    maxPerPage = maxPerPage)

  def delete(categ: Categ, topic: Topic): Funit =
    PostRepo.idsByTopicId(topic.id) flatMap { postIds =>
      (PostRepo removeByTopic topic.id zip $remove(topic)) >>
        (env.categApi denormalize categ) >>-
        (indexer ! RemovePosts(postIds)) >>
        env.recent.invalidate
    }

  def toggleClose(categ: Categ, topic: Topic, mod: User): Funit =
    TopicRepo.close(topic.id, topic.open) >> {
      MasterGranter(_.ModerateForum)(mod) ??
        modLog.toggleCloseTopic(mod, categ.name, topic.name, topic.open)
    }

  def toggleHide(categ: Categ, topic: Topic, mod: User): Funit =
    TopicRepo.hide(topic.id, topic.visibleOnHome) >> {
      MasterGranter(_.ModerateForum)(mod) ?? {
        PostRepo.hideByTopic(topic.id, topic.visibleOnHome) zip
          modLog.toggleHideTopic(mod, categ.name, topic.name, topic.visibleOnHome)
      } >> env.recent.invalidate
    }

  def denormalize(topic: Topic): Funit = for {
    nbPosts ← PostRepo countByTopics List(topic)
    lastPost ← PostRepo lastByTopics List(topic)
    nbPostsTroll ← PostRepoTroll countByTopics List(topic)
    lastPostTroll ← PostRepoTroll lastByTopics List(topic)
    _ ← $update(topic.copy(
      nbPosts = nbPosts,
      lastPostId = lastPost ?? (_.id),
      updatedAt = lastPost.fold(topic.updatedAt)(_.createdAt),
      nbPostsTroll = nbPostsTroll,
      lastPostIdTroll = lastPostTroll ?? (_.id),
      updatedAtTroll = lastPostTroll.fold(topic.updatedAtTroll)(_.createdAt)
    ))
  } yield ()

  def denormalize: Funit = $find.all[Topic] flatMap { topics =>
    topics.map(denormalize).sequenceFu
  } void
}
