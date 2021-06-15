package lila.forum

import actorApi._
import org.joda.time.DateTime
import reactivemongo.akkastream.{ cursorProducer, AkkaStreamCursor }
import reactivemongo.api.ReadPreference
import scala.util.chaining._

import lila.common.Bus
import lila.common.paginator._
import lila.db.dsl._
import lila.db.paginator._
import lila.hub.actorApi.timeline.{ ForumPost, Propagate }
import lila.hub.LightTeam.TeamID
import lila.mod.ModlogApi
import lila.security.{ Granter => MasterGranter }
import lila.user.User

final class PostApi(
    env: Env,
    indexer: lila.hub.actors.ForumSearch,
    maxPerPage: lila.common.config.MaxPerPage,
    modLog: ModlogApi,
    spam: lila.security.Spam,
    promotion: lila.security.PromotionApi,
    timeline: lila.hub.actors.Timeline,
    shutup: lila.hub.actors.Shutup,
    detectLanguage: DetectLanguage
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._

  def makePost(
      categ: Categ,
      topic: Topic,
      data: ForumForm.PostData,
      me: User
  ): Fu[Post] = {
    val number = topic.nbPosts
    detectLanguage(data.text) zip recentUserIds(topic, number) flatMap { case (lang, topicUserIds) =>
      val post = Post.make(
        topicId = topic.id,
        author = none,
        userId = me.id,
        text = spam.replace(data.text),
        number = number + 1,
        lang = lang.map(_.language),
        troll = me.marks.troll,
        hidden = topic.hidden,
        categId = categ.id,
        modIcon = (~data.modIcon && MasterGranter(_.PublicMod)(me)).option(true)
      )
      env.postRepo findDuplicate post flatMap {
        case Some(dup) if !post.modIcon.getOrElse(false) => fuccess(dup)
        case _ =>
          env.postRepo.coll.insert.one(post) >>
            env.topicRepo.coll.update.one($id(topic.id), topic withPost post) >> {
              shouldHideOnPost(topic) ?? env.topicRepo.hide(topic.id, value = true)
            } >>
            env.categRepo.coll.update.one($id(categ.id), categ.withPost(topic, post)) >>- {
              !categ.quiet ?? (indexer ! InsertPost(post))
              !categ.quiet ?? env.recent.invalidate()
              promotion.save(me, post.text)
              shutup ! {
                if (post.isTeam) lila.hub.actorApi.shutup.RecordTeamForumMessage(me.id, post.text)
                else lila.hub.actorApi.shutup.RecordPublicForumMessage(me.id, post.text)
              }
              if (!post.troll && !categ.quiet && !topic.isTooBig)
                timeline ! Propagate(ForumPost(me.id, topic.id.some, topic.name, post.id)).pipe {
                  _ toFollowersOf me.id toUsers topicUserIds exceptUser me.id
                }
              lila.mon.forum.post.create.increment()
              env.mentionNotifier.notifyMentionedUsers(post, topic)
              Bus.publish(actorApi.CreatePost(post), "forumPost")
            } inject post
      }
    }
  }

  def editPost(postId: Post.ID, newText: String, user: User): Fu[Post] =
    get(postId) flatMap { post =>
      post.fold[Fu[Post]](fufail("Post no longer exists.")) {
        case (_, post) if !post.canBeEditedBy(user.id) =>
          fufail("You are not authorized to modify this post.")
        case (_, post) if !post.canStillBeEdited =>
          fufail("Post can no longer be edited")
        case (_, post) =>
          val newPost = post.editPost(DateTime.now, spam replace newText)
          (newPost.text != post.text).?? {
            env.postRepo.coll.update.one($id(post.id), newPost).void
          } inject newPost
      }
    }

  private val quickHideCategs = Set("lichess-feedback", "off-topic-discussion")

  private def shouldHideOnPost(topic: Topic) =
    topic.visibleOnHome && {
      (quickHideCategs(topic.categId) && topic.nbPosts == 1) || {
        topic.nbPosts == maxPerPage.value ||
        (!topic.looksLikeTeamForum && topic.createdAt.isBefore(DateTime.now minusDays 5))
      }
    }

  def urlData(postId: String, forUser: Option[User]): Fu[Option[PostUrlData]] =
    get(postId) flatMap {
      case Some((_, post)) if !post.visibleBy(forUser) => fuccess(none[PostUrlData])
      case Some((topic, post)) =>
        env.postRepo.forUser(forUser).countBeforeNumber(topic.id, post.number) dmap { nb =>
          val page = nb / maxPerPage.value + 1
          PostUrlData(topic.categId, topic.slug, page, post.number).some
        }
      case _ => fuccess(none)
    }

  def get(postId: String): Fu[Option[(Topic, Post)]] =
    getPost(postId) flatMap {
      _ ?? { post =>
        env.topicRepo.coll.byId[Topic](post.topicId) dmap2 { _ -> post }
      }
    }

  def getPost(postId: String): Fu[Option[Post]] =
    env.postRepo.coll.byId[Post](postId)

  def react(postId: String, me: User, reaction: String, v: Boolean): Fu[Option[Post]] =
    Post.Reaction.set(reaction) ?? {
      if (v) lila.mon.forum.reaction(reaction).increment()
      env.postRepo.coll.ext
        .findAndUpdate[Post](
          selector = $id(postId),
          update = {
            if (v) $addToSet(s"reactions.$reaction" -> me.id)
            else $pull(s"reactions.$reaction"       -> me.id)
          },
          fetchNewObject = true
        )
    }

  def views(posts: List[Post]): Fu[List[PostView]] =
    for {
      topics <- env.topicRepo.coll.byIds[Topic](posts.map(_.topicId).distinct)
      categs <- env.categRepo.coll.byIds[Categ](topics.map(_.categId).distinct)
    } yield posts flatMap { post =>
      for {
        topic <- topics find (_.id == post.topicId)
        categ <- categs find (_.slug == topic.categId)
      } yield PostView(post, topic, categ, lastPageOf(topic))
    }

  def viewsFromIds(postIds: Seq[Post.ID]): Fu[List[PostView]] =
    env.postRepo.coll.byOrderedIds[Post, Post.ID](postIds)(_.id) flatMap views

  def viewOf(post: Post): Fu[Option[PostView]] =
    views(List(post)) dmap (_.headOption)

  def liteViews(posts: Seq[Post]): Fu[Seq[PostLiteView]] =
    env.topicRepo.coll.byIds[Topic](posts.map(_.topicId).distinct) map { topics =>
      posts flatMap { post =>
        topics.find(_.id == post.topicId) map { PostLiteView(post, _) }
      }
    }
  def liteViewsByIds(postIds: Seq[Post.ID]): Fu[Seq[PostLiteView]] =
    env.postRepo.byIds(postIds) flatMap liteViews

  def liteView(post: Post): Fu[Option[PostLiteView]] =
    liteViews(List(post)) dmap (_.headOption)

  def miniPosts(posts: List[Post]): Fu[List[MiniForumPost]] =
    env.topicRepo.coll.byIds[Topic](posts.map(_.topicId).distinct) map { topics =>
      posts flatMap { post =>
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
    }

  def lastPageOf(topic: Topic): Int =
    (topic.nbPosts + maxPerPage.value - 1) / maxPerPage.value

  def paginator(topic: Topic, page: Int, me: Option[User]): Fu[Paginator[Post]] =
    Paginator(
      new Adapter(
        collection = env.postRepo.coll,
        selector = env.postRepo.forUser(me) selectTopic topic.id,
        projection = none,
        sort = env.postRepo.sortQuery
      ),
      currentPage = page,
      maxPerPage = maxPerPage
    )

  def delete(categSlug: String, postId: String, mod: User): Funit =
    env.postRepo.unsafe.byCategAndId(categSlug, postId) flatMap {
      _ ?? { post =>
        viewOf(post) flatMap {
          _ ?? { view =>
            for {
              first <- env.postRepo.isFirstPost(view.topic.id, view.post.id)
              _ <-
                if (first) env.topicApi.delete(view.categ, view.topic)
                else
                  env.postRepo.coll.delete.one($id(view.post.id)) >>
                    (env.topicApi denormalize view.topic) >>
                    (env.categApi denormalize view.categ) >>-
                    env.recent.invalidate() >>-
                    (indexer ! RemovePost(post.id))
              _ <- MasterGranter(_.ModerateForum)(mod) ?? modLog.deletePost(
                mod.id,
                post.userId,
                post.author,
                text = "%s / %s / %s".format(view.categ.name, view.topic.name, post.text)
              )
            } yield ()
          }
        }
      }
    }

  def allUserIds(topicId: Topic.ID) = env.postRepo allUserIdsByTopicId topicId

  def nbByUser(userId: String) = env.postRepo.coll.countSel($doc("userId" -> userId))

  def allByUser(userId: User.ID): AkkaStreamCursor[Post] =
    env.postRepo.coll
      .find($doc("userId" -> userId))
      .sort($doc("createdAt" -> -1))
      .cursor[Post](ReadPreference.secondaryPreferred)

  private def recentUserIds(topic: Topic, newPostNumber: Int) =
    env.postRepo.coll
      .distinctEasy[User.ID, List](
        "userId",
        $doc(
          "topicId" -> topic.id,
          "number" $gt (newPostNumber - 10),
          "createdAt" $gt DateTime.now.minusDays(5)
        ),
        ReadPreference.secondaryPreferred
      )

  def erasePost(post: Post) =
    env.postRepo.coll.update.one($id(post.id), post.erase).void >>-
      (indexer ! RemovePost(post.id))

  def eraseFromSearchIndex(user: User): Funit =
    env.postRepo.coll
      .distinctEasy[Post.ID, List]("_id", $doc("userId" -> user.id), ReadPreference.secondaryPreferred)
      .map { ids =>
        indexer ! RemovePosts(ids)
      }

  def teamIdOfPostId(postId: Post.ID): Fu[Option[TeamID]] =
    env.postRepo.coll.byId[Post](postId) flatMap {
      _ ?? { post =>
        env.categRepo.coll.primitiveOne[TeamID]($id(post.categId), "team")
      }
    }
}
