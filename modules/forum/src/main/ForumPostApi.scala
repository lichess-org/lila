package lila.forum

import reactivemongo.api.ReadPreference
import scala.util.chaining.*

import lila.common.Bus
import lila.db.dsl.{ *, given }
import lila.hub.actorApi.timeline.{ ForumPost as TimelinePost, Propagate }
import lila.security.{ Granter as MasterGranter }
import lila.user.User

final class ForumPostApi(
    postRepo: ForumPostRepo,
    topicRepo: ForumTopicRepo,
    categRepo: ForumCategRepo,
    mentionNotifier: MentionNotifier,
    indexer: lila.hub.actors.ForumSearch,
    config: ForumConfig,
    modLog: lila.mod.ModlogApi,
    spam: lila.security.Spam,
    promotion: lila.security.PromotionApi,
    timeline: lila.hub.actors.Timeline,
    shutup: lila.hub.actors.Shutup,
    detectLanguage: DetectLanguage
)(using Executor):

  import BSONHandlers.given

  def makePost(
      categ: ForumCateg,
      topic: ForumTopic,
      data: ForumForm.PostData,
      me: User
  ): Fu[ForumPost] =
    detectLanguage(data.text) zip recentUserIds(topic, topic.nbPosts) flatMap { case (lang, topicUserIds) =>
      val publicMod = MasterGranter(_.PublicMod)(me)
      val modIcon   = ~data.modIcon && (publicMod || MasterGranter(_.SeeReport)(me))
      val anonMod   = modIcon && !publicMod
      val post = ForumPost.make(
        topicId = topic.id,
        userId = !anonMod option me.id,
        text = spam.replace(data.text),
        number = topic.nbPosts + 1,
        lang = lang.map(_.language),
        troll = me.marks.troll,
        categId = categ.id,
        modIcon = modIcon option true
      )
      postRepo findDuplicate post flatMap {
        case Some(dup) if !post.modIcon.getOrElse(false) => fuccess(dup)
        case _ =>
          postRepo.coll.insert.one(post) >>
            topicRepo.coll.update.one($id(topic.id), topic withPost post) >>
            categRepo.coll.update.one($id(categ.id), categ.withPost(topic, post)) >>- {
              !categ.quiet ?? (indexer ! InsertPost(post))
              promotion.save(me, post.text)
              shutup ! {
                if (post.isTeam) lila.hub.actorApi.shutup.RecordTeamForumMessage(me.id, post.text)
                else lila.hub.actorApi.shutup.RecordPublicForumMessage(me.id, post.text)
              }
              if (anonMod) logAnonPost(me.id, post, edit = false)
              else if (!post.troll && !categ.quiet)
                timeline ! Propagate(TimelinePost(me.id, topic.id, topic.name, post.id)).pipe {
                  _ toFollowersOf me.id toUsers topicUserIds exceptUser me.id withTeam categ.team
                }
              lila.mon.forum.post.create.increment()
              mentionNotifier.notifyMentionedUsers(post, topic)
              Bus.publish(CreatePost(post), "forumPost")
            } inject post
      }
    }

  def editPost(postId: ForumPostId, newText: String, user: User): Fu[ForumPost] =
    get(postId) flatMap { post =>
      post.fold[Fu[ForumPost]](fufail("Post no longer exists.")) {
        case (_, post) if !post.canBeEditedBy(user) =>
          fufail("You are not authorized to modify this post.")
        case (_, post) if !post.canStillBeEdited =>
          fufail("Post can no longer be edited")
        case (_, post) =>
          val newPost = post.editPost(nowInstant, spam replace newText)
          (newPost.text != post.text).?? {
            postRepo.coll.update.one($id(post.id), newPost) >> newPost.isAnonModPost.?? {
              logAnonPost(user.id, newPost, edit = true)
            } >>- promotion.save(user, newPost.text)
          } inject newPost
      }
    }

  def urlData(postId: ForumPostId, forUser: Option[User]): Fu[Option[PostUrlData]] =
    get(postId) flatMap {
      case Some((_, post)) if !post.visibleBy(forUser) => fuccess(none[PostUrlData])
      case Some((topic, post)) =>
        postRepo.forUser(forUser).countBeforeNumber(topic.id, post.number) dmap { nb =>
          val page = nb / config.postMaxPerPage.value + 1
          PostUrlData(topic.categId, topic.slug, page, post.number).some
        }
      case _ => fuccess(none)
    }

  def get(postId: ForumPostId): Fu[Option[(ForumTopic, ForumPost)]] =
    getPost(postId) flatMapz { post =>
      topicRepo.byId(post.topicId) dmap2 { _ -> post }
    }

  def getPost(postId: ForumPostId): Fu[Option[ForumPost]] =
    postRepo.coll.byId[ForumPost](postId)

  def react(
      categSlug: String,
      postId: ForumPostId,
      me: User,
      reaction: String,
      v: Boolean
  ): Fu[Option[ForumPost]] =
    ForumPost.Reaction.set(reaction) ?? {
      if (v) lila.mon.forum.reaction(reaction).increment()
      postRepo.coll
        .findAndUpdateSimplified[ForumPost](
          selector = $id(postId) ++ $doc("categId" -> categSlug, "userId" $ne me.id),
          update = {
            if (v) $addToSet(s"reactions.$reaction" -> me.id)
            else $pull(s"reactions.$reaction"       -> me.id)
          },
          fetchNewObject = true
        )
    }

  def views(posts: List[ForumPost]): Fu[List[PostView]] =
    for
      topics <- topicRepo.coll.byIds[ForumTopic, ForumTopicId](posts.map(_.topicId).distinct)
      categs <- categRepo.coll.byIds[ForumCateg, ForumCategId](topics.map(_.categId).distinct)
    yield posts flatMap { post =>
      for
        topic <- topics.find(_.id == post.topicId)
        categ <- categs.find(_.id == topic.categId)
      yield PostView(post, topic, categ)
    }

  def viewsFromIds(postIds: Seq[ForumPostId]): Fu[List[PostView]] =
    postRepo.coll.byOrderedIds[ForumPost, ForumPostId](postIds)(_.id) flatMap views

  def viewOf(post: ForumPost): Fu[Option[PostView]] =
    views(List(post)).dmap(_.headOption)

  def liteViews(posts: Seq[ForumPost]): Fu[Seq[PostLiteView]] =
    topicRepo.coll.byStringIds[ForumTopic](posts.map(_.topicId.value).distinct) map { topics =>
      posts.flatMap { post =>
        topics.find(_.id == post.topicId) map { PostLiteView(post, _) }
      }
    }
  def liteViewsByIds(postIds: Seq[ForumPostId]): Fu[Seq[PostLiteView]] =
    postRepo.byIds(postIds) flatMap liteViews

  def liteView(post: ForumPost): Fu[Option[PostLiteView]] =
    liteViews(List(post)) dmap (_.headOption)

  def miniPosts(posts: List[ForumPost]): Fu[List[MiniForumPost]] =
    topicRepo.coll.byStringIds[ForumTopic](posts.map(_.topicId.value).distinct) map { topics =>
      posts flatMap { post =>
        topics.find(_.id == post.topicId) map { topic =>
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

  def allUserIds(topicId: ForumTopicId) = postRepo allUserIdsByTopicId topicId

  def nbByUser(userId: UserId) = postRepo.coll.countSel($doc("userId" -> userId))

  def categsForUser(teams: Iterable[TeamId], forUser: Option[User]): Fu[List[CategView]] =
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
      }.parallel
    } yield views

  private def recentUserIds(topic: ForumTopic, newPostNumber: Int) =
    postRepo.coll
      .distinctEasy[UserId, List](
        "userId",
        $doc(
          "topicId" -> topic.id,
          "number" $gt (newPostNumber - 20)
        ),
        ReadPreference.secondaryPreferred
      )

  def erasePost(post: ForumPost) =
    postRepo.coll.update.one($id(post.id), post.erase).void >>-
      (indexer ! RemovePost(post.id))

  def eraseFromSearchIndex(user: User): Funit =
    postRepo.coll
      .distinctEasy[ForumPostId, List]("_id", $doc("userId" -> user.id), ReadPreference.secondaryPreferred)
      .map { ids =>
        indexer ! RemovePosts(ids)
      }

  def teamIdOfPostId(postId: ForumPostId): Fu[Option[TeamId]] =
    postRepo.coll.byId[ForumPost](postId) flatMapz { post =>
      categRepo.coll.primitiveOne[TeamId]($id(post.categId), "team")
    }

  private def logAnonPost(userId: UserId, post: ForumPost, edit: Boolean): Funit =
    topicRepo.byId(post.topicId) orFail s"No such topic ${post.topicId}" flatMap { topic =>
      modLog.postOrEditAsAnonMod(
        userId into ModId,
        post.categId,
        topic.slug,
        post.id,
        post.text,
        edit
      )
    }
