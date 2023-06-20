package controllers

import views.*

import lila.app.{ given, * }
import lila.common.IpAddress
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
            paginator <- env.forumSearch(text, page, ctx.troll)
            posts <- paginator.mapFutureResults: post =>
              access.isGrantedRead(post.categ.id) map { canRead =>
                lila.forum.PostView.WithReadPerm(post, canRead)
              }
          yield html.forum.search(text, posts)

  def create(categId: ForumCategId, slug: String, page: Int) = AuthBody { ctx ?=> me ?=>
    NoBot:
      OptionFuResult(topicApi.show(categId, slug, page)): (categ, topic, posts) =>
        if topic.closed then BadRequest("This topic is closed")
        else if topic.isOld then BadRequest("This topic is archived")
        else
          categ.team.so(env.team.cached.isLeader(_, me)) flatMap { inOwnTeam =>
            forms
              .post(inOwnTeam)
              .bindFromRequest()
              .fold(
                err =>
                  CategGrantWrite(categId, tryingToPostAsMod = true):
                    for
                      captcha     <- forms.anyCaptcha
                      unsub       <- env.timeline.status(s"forum:${topic.id}")
                      canModCateg <- access.isGrantedMod(categ.slug)
                    yield BadRequest:
                      html.forum.topic
                        .show(categ, topic, posts, Some(err -> captcha), unsub, canModCateg = canModCateg)
                ,
                data =>
                  CategGrantWrite(categId, tryingToPostAsMod = ~data.modIcon):
                    CreateRateLimit(ctx.ip, rateLimitedFu):
                      postApi.makePost(categ, topic, data) map { post =>
                        Redirect(routes.ForumPost.redirect(post.id))
                      }
              )
          }
  }

  def edit(postId: ForumPostId) = AuthBody { ctx ?=> me ?=>
    env.forum.postApi.teamIdOfPostId(postId) flatMap { teamId =>
      teamId.so(env.team.cached.isLeader(_, me)) flatMap { inOwnTeam =>
        postApi getPost postId flatMapz { post =>
          forms
            .postEdit(inOwnTeam, post.text)
            .bindFromRequest()
            .fold(
              _ => Redirect(routes.ForumPost.redirect(postId)),
              data =>
                CreateRateLimit(ctx.ip, rateLimitedFu):
                  postApi.editPost(postId, data.changes).map { post =>
                    Redirect(routes.ForumPost.redirect(post.id))
                  }
            )
        }
      }
    }
  }

  def delete(categId: ForumCategId, id: ForumPostId) = AuthBody { ctx ?=> me ?=>
    postApi getPost id flatMapz { post =>
      if (post.userId.exists(_ is me) && !post.erased)
        postApi.erasePost(post) inject Redirect(routes.ForumPost.redirect(id))
      else
        TopicGrantModById(categId, post.topicId):
          env.forum.delete
            .post(categId, id)
            .inject:
              for
                userId    <- post.userId
                reasonOpt <- forms.deleteWithReason.bindFromRequest().value
                topic     <- topicRepo.forUser(me.some).byId(post.topicId)
                reason    <- reasonOpt.filter(MsgPreset.forumDeletion.presets.contains)
                preset =
                  if (isGranted(_.ModerateForum)) MsgPreset.forumDeletion.byModerator
                  else if (topic.exists(_ isUblogAuthor me))
                    MsgPreset.forumDeletion.byBlogAuthor(me.username)
                  else MsgPreset.forumDeletion.byTeamLeader(categId)
              do env.msg.api.systemPost(userId, preset(reason))
              NoContent
    }
  }

  def react(categId: ForumCategId, id: ForumPostId, reaction: String, v: Boolean) = Auth { _ ?=> me ?=>
    CategGrantWrite(categId):
      postApi.react(categId, id, reaction, v) mapz { post =>
        Ok(views.html.forum.post.reactions(post, canReact = true))
      }
  }

  def redirect(id: ForumPostId) = Open:
    OptionResult(postApi.urlData(id, ctx.me)) { case lila.forum.PostUrlData(categ, topic, page, number) =>
      val call = routes.ForumTopic.show(categ, topic, page)
      Redirect(s"$call#$number").withCanonical(call)
    }
