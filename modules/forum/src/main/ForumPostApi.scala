package lila.forum

import lila.common.Bus
import lila.core.forum.{ BusForum, ForumCateg as _, ForumPost as _, * }
import lila.core.perm.Granter as MasterGranter
import lila.core.shutup.{ PublicSource, ShutupApi }
import lila.core.timeline.{ ForumPost as TimelinePost, Propagate }
import lila.db.dsl.{ *, given }

final class ForumPostApi(
    postRepo: ForumPostRepo,
    topicRepo: ForumTopicRepo,
    categRepo: ForumCategRepo,
    mentionNotifier: MentionNotifier,
    modLog: lila.core.mod.LogApi,
    config: ForumConfig,
    spam: lila.core.security.SpamApi,
    promotion: lila.core.security.PromotionApi,
    shutupApi: lila.core.shutup.ShutupApi,
    detectLanguage: DetectLanguage
)(using Executor)(using scheduler: Scheduler)
    extends lila.core.forum.ForumPostApi:

  import BSONHandlers.given

  def makePost(
      categ: ForumCateg,
      topic: ForumTopic,
      data: ForumForm.PostData
  )(using me: Me): Fu[ForumPost] =
    detectLanguage(data.text).zip(recentUserIds(topic, topic.nbPosts)).flatMap { (lang, topicUserIds) =>
      val publicMod = MasterGranter(_.PublicMod)
      val modIcon = ~data.modIcon && (publicMod || MasterGranter(_.SeeReport))
      val anonMod = modIcon && !publicMod
      val post = ForumPost.make(
        topicId = topic.id,
        userId = (!anonMod).option(me),
        text = spam.replace(data.text),
        number = topic.nbPosts + 1,
        lang = lang.map(_.language),
        troll = me.marks.troll,
        categId = categ.id,
        modIcon = modIcon.option(true)
      )
      postRepo
        .findDuplicate(post)
        .flatMap:
          case Some(dup) if !post.modIcon.getOrElse(false) => fuccess(dup)
          case _ =>
            for
              _ <- postRepo.coll.insert.one(post)
              _ <- topicRepo.coll.update.one($id(topic.id), topic.withPost(post))
              _ <- categRepo.coll.update.one($id(categ.id), categ.withPost(topic, post))
            yield
              promotion.save(me, post.text)
              if post.isTeam
              then shutupApi.teamForumMessage(me, post.text)
              else shutupApi.publicText(me, post.text, PublicSource.Forum(post.id))
              if anonMod
              then logAnonPost(post, edit = false)
              else if !post.troll && !categ.quiet then
                lila.common.Bus.pub:
                  Propagate(TimelinePost(me, topic.id, topic.name, post.id))
                    .toFollowersOf(me)
                    .toUsers(topicUserIds)
                    .exceptUser(me)
                    .withTeam(categ.team)
              else if categ.id == ForumCateg.diagnosticId then
                lila.common.Bus.pub:
                  Propagate(TimelinePost(me, topic.id, topic.name, post.id))
                    .toUsers(topicUserIds)
                    .exceptUser(me)
              lila.mon.forum.post.create.increment()
              mentionNotifier.notifyMentionedUsers(post, topic)
              Bus.pub(BusForum.CreatePost(post.mini))
              post
    }

  def editPost(postId: ForumPostId, newText: String)(using me: Me): Fu[ForumPost] =
    get(postId).flatMap: post =>
      post.fold[Fu[ForumPost]](fufail("Post no longer exists.")):
        case (_, post) if !post.canBeEditedByMe =>
          fufail("You are not authorized to modify this post.")
        case (_, post) if !post.canStillBeEdited =>
          fufail("Post can no longer be edited")
        case (_, post) =>
          val newPost = post.editPost(nowInstant, spam.replace(newText))
          val save = (newPost.text != post.text).so:
            for
              _ <- postRepo.coll.update.one($id(post.id), newPost)
              _ <- newPost.isAnonModPost.so(logAnonPost(newPost, edit = true))
            yield promotion.save(me, newPost.text)
          save.inject(newPost)

  def urlData(postId: ForumPostId, forUser: Option[User]): Fu[Option[PostUrlData]] =
    get(postId).flatMap:
      case Some(_, post) if !post.visibleBy(forUser) => fuccess(none[PostUrlData])
      case Some(topic, post) =>
        postRepo.forUser(forUser).countBeforeNumber(topic.id, post.number).dmap { nb =>
          val page = nb / config.postMaxPerPage.value + 1
          PostUrlData(topic.categId, topic.slug, page, post.number).some
        }
      case _ => fuccess(none)

  def get(postId: ForumPostId): Fu[Option[(ForumTopic, ForumPost)]] =
    getPost(postId).flatMapz: post =>
      topicRepo.byId(post.topicId).dmap2(_ -> post)

  def getPost(postId: ForumPostId): Fu[Option[ForumPost]] =
    postRepo.coll.byId[ForumPost](postId)

  def react(
      categId: ForumCategId,
      postId: ForumPostId,
      reactionStr: String,
      v: Boolean
  )(using me: Me): Fu[Option[ForumPost]] =
    ForumPost.Reaction(reactionStr).so { reaction =>
      if v then lila.mon.forum.reaction(reaction.key).increment()
      for
        post <- postRepo.coll
          .findAndUpdateSimplified[ForumPost](
            selector = $id(postId) ++ $doc("categId" -> categId, "userId".$ne(me.userId)),
            update =
              if v then $addToSet(s"reactions.$reaction" -> me.userId)
              else $pull(s"reactions.$reaction" -> me.userId),
            fetchNewObject = true
          )
        _ =
          if me.marks.troll && reaction == ForumPost.Reaction.MinusOne && v
          then
            scheduler.scheduleOnce(5.minutes):
              react(categId, postId, reaction.key, false)
      yield post
    }

  def views(posts: Seq[ForumPost]): Fu[List[PostView]] =
    for
      topics <- topicRepo.coll.byIds[ForumTopic, ForumTopicId](posts.map(_.topicId).distinct)
      categs <- categRepo.coll.byIds[ForumCateg, ForumCategId](topics.map(_.categId).distinct)
    yield posts.toList.flatMap: post =>
      for
        topic <- topics.find(_.id == post.topicId)
        categ <- categs.find(_.id == topic.categId)
      yield PostView(post, topic, categ)

  def viewsFromIds(postIds: Seq[ForumPostId]): Fu[List[PostView]] =
    postRepo.coll.byOrderedIds[ForumPost, ForumPostId](postIds)(_.id).flatMap(views)

  def viewOf(post: ForumPost): Fu[Option[PostView]] =
    views(List(post)).dmap(_.headOption)

  def miniViews(postIds: List[ForumPostId]): Fu[List[ForumPostMiniView]] = postIds.nonEmpty.so:
    for
      posts <- postRepo.miniByIds(postIds)
      topics <- topicRepo.coll.byStringIds[ForumTopicMini](posts.map(_.topicId.value).distinct)
    yield posts.flatMap: post =>
      topics.find(_.id == post.topicId).map { ForumPostMiniView(post, _) }

  def toMiniViews(posts: List[ForumPostMini]): Fu[List[ForumPostMiniView]] = posts.nonEmpty.so:
    topicRepo
      .byIds(posts.map(_.topicId))
      .map: topics =>
        posts.flatMap: post =>
          topics.find(_.id == post.topicId).map { ForumPostMiniView(post, _) }

  def toMiniView(post: ForumPost): Fu[Option[ForumPostMiniView]] =
    toMiniViews(List(post.mini)).dmap(_.headOption)

  def toMiniView(post: ForumPostMini): Fu[Option[ForumPostMiniView]] =
    miniViews(List(post.id)).dmap(_.headOption)

  def miniPosts(posts: List[ForumPost]): Fu[List[ForumPostMiniView]] =
    topicRepo
      .byIds(posts.map(_.topicId))
      .map: topics =>
        posts.flatMap: post =>
          topics
            .find(_.id == post.topicId)
            .map: topic =>
              ForumPostMiniView(post = post.mini, topic = topic)

  def allUserIds(topicId: ForumTopicId) = postRepo.allUserIdsByTopicId(topicId)

  def nbByUser(userId: UserId) = postRepo.coll.countSel($doc("userId" -> userId))

  def categsForUser(teams: Iterable[TeamId], forUser: Option[User]): Fu[List[CategView]] =
    val isMod = forUser.fold(false)(MasterGranter.of(_.ModerateForum))
    for
      categs <- categRepo.visibleWithTeams(teams, isMod)
      diagnostic <- if isMod then fuccess(none) else forUser.so(diagnosticForUser)
      views <- categs
        .parallel: categ =>
          get(categ.lastPostId(forUser)).map: topicPost =>
            CategView(
              categ,
              topicPost.map { case (topic, post) => (topic, post, topic.lastPage(config.postMaxPerPage)) },
              forUser
            )
    yield views ++ diagnostic.toList

  private def diagnosticForUser(user: User): Fu[Option[CategView]] = // CategView with user's topic/post
    for
      categOpt <- categRepo.byId(ForumCateg.diagnosticId)
      topicOpt <- topicRepo.byTree(ForumCateg.diagnosticId, ForumTopic.problemReportSlug(user.id))
      postOpt <- topicOpt.so(t => postRepo.coll.byId[ForumPost](t.lastPostId(user.some)))
    yield for
      post <- postOpt
      topic <- topicOpt
      categ <- categOpt
    yield CategView(categ, (topic, post, topic.lastPage(config.postMaxPerPage)).some, user.some)

  private def recentUserIds(topic: ForumTopic, newPostNumber: Int) =
    postRepo.coll
      .distinctEasy[UserId, List](
        "userId",
        $doc(
          "topicId" -> topic.id,
          "number".$gt(newPostNumber - 20)
        ),
        _.sec
      )

  def erasePost(post: ForumPost) =
    postRepo.coll.update
      .one($id(post.id), post.erase)
      .void

  def teamIdOfPost(post: ForumPost): Fu[Option[TeamId]] =
    categRepo.coll.primitiveOne[TeamId]($id(post.categId), "team")

  private def logAnonPost(post: ForumPost, edit: Boolean)(using Me): Funit =
    topicRepo.byId(post.topicId).orFail(s"No such topic ${post.topicId}").flatMap { topic =>
      modLog.postOrEditAsAnonMod(
        post.categId,
        topic.slug,
        post.id,
        post.text,
        edit
      )
    }
