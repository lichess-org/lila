package lila.forum

import actorApi._
import org.joda.time.DateTime
import reactivemongo.akkastream.{ cursorProducer, AkkaStreamCursor }
import reactivemongo.api.ReadPreference
import scala.util.chaining._

import lila.common.Bus
import lila.db.dsl._
import lila.hub.actorApi.timeline.{ ForumPost, Propagate }
import lila.hub.LightTeam.TeamID
import lila.security.{ Granter => MasterGranter }
import lila.user.User

final class PostApi(
    postRepo: PostRepo,
    topicRepo: TopicRepo,
    categRepo: CategRepo,
    mentionNotifier: MentionNotifier,
    indexer: lila.hub.actors.ForumSearch,
    config: ForumConfig,
    modLog: lila.mod.ModlogApi,
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
  ): Fu[Post] =
    detectLanguage(data.text) zip recentUserIds(topic, topic.nbPosts) flatMap { case (lang, topicUserIds) =>
      val publicMod = MasterGranter(_.PublicMod)(me)
      val modIcon   = ~data.modIcon && (publicMod || MasterGranter(_.SeeReport)(me))
      val anonMod   = modIcon && !publicMod
      val post = Post.make(
        topicId = topic.id,
        author = none,
        userId = !anonMod option me.id,
        text = spam.replace(data.text),
        number = topic.nbPosts + 1,
        lang = lang.map(_.language),
        troll = me.marks.troll,
        hidden = topic.hidden,
        categId = categ.id,
        modIcon = modIcon option true
      )
      postRepo findDuplicate post flatMap {
        case Some(dup) if !post.modIcon.getOrElse(false) => fuccess(dup)
        case _ =>
          postRepo.coll.insert.one(post) >>
            topicRepo.coll.update.one($id(topic.id), topic withPost post) >> {
              shouldHideOnPost(topic) ?? topicRepo.hide(topic.id, value = true)
            } >>
            categRepo.coll.update.one($id(categ.id), categ.withPost(topic, post)) >>- {
              !categ.quiet ?? (indexer ! InsertPost(post))
              promotion.save(me, post.text)
              shutup ! {
                if (post.isTeam) lila.hub.actorApi.shutup.RecordTeamForumMessage(me.id, post.text)
                else lila.hub.actorApi.shutup.RecordPublicForumMessage(me.id, post.text)
              }
              if (anonMod) logAnonPost(me.id, post, edit = false)
              else if (!post.troll && !categ.quiet && !topic.isTooBig)
                timeline ! Propagate(ForumPost(me.id, topic.id.some, topic.name, post.id)).pipe {
                  _ toFollowersOf me.id toUsers topicUserIds exceptUser me.id
                }
              lila.mon.forum.post.create.increment()
              mentionNotifier.notifyMentionedUsers(post, topic)
              Bus.publish(actorApi.CreatePost(post), "forumPost")
            } inject post
      }
    }

  def editPost(postId: Post.ID, newText: String, user: User): Fu[Post] =
    get(postId) flatMap { post =>
      post.fold[Fu[Post]](fufail("Post no longer exists.")) {
        case (_, post) if !post.canBeEditedBy(user) =>
          fufail("You are not authorized to modify this post.")
        case (_, post) if !post.canStillBeEdited =>
          fufail("Post can no longer be edited")
        case (_, post) =>
          val newPost = post.editPost(DateTime.now, spam replace newText)
          (newPost.text != post.text).?? {
            postRepo.coll.update.one($id(post.id), newPost) >> newPost.isAnonModPost.?? {
              logAnonPost(user.id, newPost, edit = true)
            } >>- promotion.save(user, newPost.text)
          } inject newPost
      }
    }

  private val quickHideCategs = Set("lichess-feedback", "off-topic-discussion")

  private def shouldHideOnPost(topic: Topic) =
    topic.visibleOnHome && {
      (quickHideCategs(topic.categId) && topic.nbPosts == 1) || {
        topic.nbPosts == config.postMaxPerPage.value ||
        (!topic.looksLikeTeamForum && topic.createdAt.isBefore(DateTime.now minusDays 5))
      }
    }

  def urlData(postId: Post.ID, forUser: Option[User]): Fu[Option[PostUrlData]] =
    get(postId) flatMap {
      case Some((_, post)) if !post.visibleBy(forUser) => fuccess(none[PostUrlData])
      case Some((topic, post)) =>
        postRepo.forUser(forUser).countBeforeNumber(topic.id, post.number) dmap { nb =>
          val page = nb / config.postMaxPerPage.value + 1
          PostUrlData(topic.categId, topic.slug, page, post.number).some
        }
      case _ => fuccess(none)
    }

  def get(postId: Post.ID): Fu[Option[(Topic, Post)]] =
    getPost(postId) flatMap {
      _ ?? { post =>
        topicRepo.byId(post.topicId) dmap2 { _ -> post }
      }
    }

  def getPost(postId: Post.ID): Fu[Option[Post]] =
    postRepo.coll.byId[Post](postId)

  def react(categSlug: String, postId: Post.ID, me: User, reaction: String, v: Boolean): Fu[Option[Post]] =
    Post.Reaction.set(reaction) ?? {
      if (v) lila.mon.forum.reaction(reaction).increment()
      postRepo.coll.ext
        .findAndUpdate[Post](
          selector = $id(postId) ++ $doc("categId" -> categSlug, "userId" $ne me.id),
          update = {
            if (v) $addToSet(s"reactions.$reaction" -> me.id)
            else $pull(s"reactions.$reaction"       -> me.id)
          },
          fetchNewObject = true
        )
    }

  def views(posts: List[Post]): Fu[List[PostView]] =
    for {
      topics <- topicRepo.coll.byIds[Topic](posts.map(_.topicId).distinct)
      categs <- categRepo.coll.byIds[Categ](topics.map(_.categId).distinct)
    } yield posts flatMap { post =>
      for {
        topic <- topics find (_.id == post.topicId)
        categ <- categs find (_.slug == topic.categId)
      } yield PostView(post, topic, categ)
    }

  def viewsFromIds(postIds: Seq[Post.ID]): Fu[List[PostView]] =
    postRepo.coll.byOrderedIds[Post, Post.ID](postIds)(_.id) flatMap views

  def viewOf(post: Post): Fu[Option[PostView]] =
    views(List(post)) dmap (_.headOption)

  def liteViews(posts: Seq[Post]): Fu[Seq[PostLiteView]] =
    topicRepo.coll.byIds[Topic](posts.map(_.topicId).distinct) map { topics =>
      posts flatMap { post =>
        topics.find(_.id == post.topicId) map { PostLiteView(post, _) }
      }
    }
  def liteViewsByIds(postIds: Seq[Post.ID]): Fu[Seq[PostLiteView]] =
    postRepo.byIds(postIds) flatMap liteViews

  def liteView(post: Post): Fu[Option[PostLiteView]] =
    liteViews(List(post)) dmap (_.headOption)

  def miniPosts(posts: List[Post]): Fu[List[MiniForumPost]] =
    topicRepo.coll.byIds[Topic](posts.map(_.topicId).distinct) map { topics =>
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

  def allUserIds(topicId: Topic.ID) = postRepo allUserIdsByTopicId topicId

  def nbByUser(userId: String) = postRepo.coll.countSel($doc("userId" -> userId))

  def allByUser(userId: User.ID): AkkaStreamCursor[Post] =
    postRepo.coll
      .find($doc("userId" -> userId))
      .sort($doc("createdAt" -> -1))
      .cursor[Post](ReadPreference.secondaryPreferred)

  def categsForUser(teams: Iterable[String], forUser: Option[User]): Fu[List[CategView]] =
    for {
      categs <- categRepo visibleWithTeams teams
      views <- categs.map { categ =>
        get(categ lastPostId forUser) map { topicPost =>
          CategView(
            categ,
            topicPost map { case (topic, post) =>
              (topic, post, topic lastPage config.postMaxPerPage)
            },
            forUser
          )
        }
      }.sequenceFu
    } yield views

  private def recentUserIds(topic: Topic, newPostNumber: Int) =
    postRepo.coll
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
    postRepo.coll.update.one($id(post.id), post.erase).void >>-
      (indexer ! RemovePost(post.id))

  def eraseFromSearchIndex(user: User): Funit =
    postRepo.coll
      .distinctEasy[Post.ID, List]("_id", $doc("userId" -> user.id), ReadPreference.secondaryPreferred)
      .map { ids =>
        indexer ! RemovePosts(ids)
      }

  def teamIdOfPostId(postId: Post.ID): Fu[Option[TeamID]] =
    postRepo.coll.byId[Post](postId) flatMap {
      _ ?? { post =>
        categRepo.coll.primitiveOne[TeamID]($id(post.categId), "team")
      }
    }

  private def logAnonPost(userId: User.ID, post: Post, edit: Boolean): Funit =
    topicRepo.byId(post.topicId) orFail s"No such topic ${post.topicId}" flatMap { topic =>
      modLog.postOrEditAsAnonMod(
        userId,
        post.categId,
        topic.slug,
        post.id,
        post.text,
        edit
      )
    }
}
