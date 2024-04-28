package controllers

import lila.app.{ *, given }
import lila.core.net.IpAddress
import lila.core.i18n.I18nKey as trans
import lila.core.id.{ ForumCategId, ForumTopicId }
import lila.msg.MsgPreset

final class ForumPost(env: Env) extends LilaController(env) with ForumController:

  private val CreateRateLimit =
    lila.memo.RateLimit[IpAddress](
      credits = 4,
      duration = 5.minutes,
      key = "forum.post",
      enforce = env.net.rateLimit.value
    )

  def search(text: String, page: Int) =
    OpenBody:
      NotForKids:
        if text.trim.isEmpty
        then Redirect(routes.ForumCateg.index)
        else
          for
            ids   <- env.forumSearch(text, page, ctx.troll)
            posts <- ids.mapFutureList(env.forum.postApi.viewsFromIds)
            pager <- posts.mapFutureResults: post =>
              access.isGrantedRead(post.topic.categId).map {
                lila.forum.PostView.WithReadPerm(post, _)
              }
            page <- renderPage(views.forum.post.search(text, pager))
          yield Ok(page)

  def create(categId: ForumCategId, slug: String, page: Int) = AuthBody { ctx ?=> me ?=>
    NoBot:
      Found(topicApi.show(categId, slug, page)): (categ, topic, posts) =>
        if topic.closed then BadRequest("This topic is closed")
        else if topic.isOld then BadRequest("This topic is archived")
        else
          for
            canModCateg  <- access.isGrantedMod(categ.slug)
            replyBlocked <- access.isReplyBlockedOnUBlog(topic, canModCateg)
            res <-
              if replyBlocked then fuccess(BadRequest(trans.ublog.youBlockedByBlogAuthor()))
              else
                categ.team.so(env.team.api.isLeader(_, me)).flatMap { inOwnTeam =>
                  forms
                    .post(inOwnTeam)
                    .bindFromRequest()
                    .fold(
                      err =>
                        CategGrantWrite(categId, tryingToPostAsMod = true):
                          for
                            unsub       <- env.timeline.status(s"forum:${topic.id}")
                            canModCateg <- access.isGrantedMod(categ.slug)
                            page <- renderPage:
                              views.forum.topic
                                .show(
                                  categ,
                                  topic,
                                  posts,
                                  Some(err -> anyCaptcha),
                                  unsub,
                                  canModCateg = canModCateg
                                )
                          yield BadRequest(page)
                      ,
                      data =>
                        CategGrantWrite(categId, tryingToPostAsMod = ~data.modIcon):
                          CreateRateLimit(ctx.ip, rateLimited):
                            postApi.makePost(categ, topic, data).map { post =>
                              Redirect(routes.ForumPost.redirect(post.id))
                            }
                    )
                }
          yield res
  }

  def edit(postId: ForumPostId) = AuthBody { ctx ?=> me ?=>
    env.forum.postApi.teamIdOfPostId(postId).flatMap { teamId =>
      teamId.so(env.team.api.isLeader(_, me)).flatMap { inOwnTeam =>
        Found(postApi.getPost(postId)): post =>
          forms
            .postEdit(inOwnTeam, post.text)
            .bindFromRequest()
            .fold(
              _ => Redirect(routes.ForumPost.redirect(postId)),
              data =>
                CreateRateLimit(ctx.ip, rateLimited):
                  postApi.editPost(postId, data.changes).map { post =>
                    Redirect(routes.ForumPost.redirect(post.id))
                  }
            )
      }
    }
  }

  def delete(id: ForumPostId) = AuthBody { ctx ?=> me ?=>
    Found(postApi.getPost(id).flatMapz(postApi.viewOf)): view =>
      val post = view.post
      if post.userId.exists(_.is(me)) && !post.erased then
        if view.topic.nbPosts == 1 then
          env.forum.delete.deleteTopic(view).inject(Redirect(routes.ForumCateg.show(view.categ.slug)))
        else postApi.erasePost(post).inject(Redirect(routes.ForumPost.redirect(id)))
      else
        TopicGrantModById(post.categId, post.topicId):
          env.forum.delete
            .deletePost(view)
            .inject:
              for
                userId    <- post.userId
                reasonOpt <- forms.deleteWithReason.bindFromRequest().value
                topic     <- topicRepo.forUser(me.some).byId(post.topicId)
                reason    <- reasonOpt.filter(MsgPreset.forumDeletion.presets.contains)
                preset =
                  if isGranted(_.ModerateForum) then MsgPreset.forumDeletion.byModerator
                  else if topic.exists(_.isUblogAuthor(me)) then
                    MsgPreset.forumDeletion.byBlogAuthor(me.username)
                  else MsgPreset.forumDeletion.byTeamLeader(post.categId)
              do env.msg.api.systemPost(userId, preset(reason, view.logFormatted))
              NoContent
  }

  def react(categId: ForumCategId, id: ForumPostId, reaction: String, v: Boolean) = Auth { _ ?=> me ?=>
    CategGrantWrite(categId):
      FoundPage(postApi.react(categId, id, reaction, v)): post =>
        views.forum.post.reactions(post, canReact = true)
  }

  def redirect(id: ForumPostId) = Open:
    Found(postApi.urlData(id, ctx.me)):
      case lila.forum.PostUrlData(categ, topic, page, number) =>
        val call = routes.ForumTopic.show(categ, topic, page)
        Redirect(s"$call#$number").withCanonical(call)
