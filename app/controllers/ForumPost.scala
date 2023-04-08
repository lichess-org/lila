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
    OpenBody { implicit ctx =>
      NotForKids {
        if (text.trim.isEmpty) Redirect(routes.ForumCateg.index).toFuccess
        else
          for {
            paginator <- env.forumSearch(text, page, ctx.troll)
            posts <- paginator.mapFutureResults(post =>
              access.isGrantedRead(post.categ.id) map { canRead =>
                lila.forum.PostView.WithReadPerm(post, canRead)
              }
            )
          } yield html.forum.search(text, posts)
      }
    }

  def create(categId: ForumCategId, slug: String, page: Int) =
    AuthBody { implicit ctx => me =>
      NoBot {
        given play.api.mvc.Request[?] = ctx.body
        OptionFuResult(topicApi.show(categId, slug, page, ctx.me)) { case (categ, topic, posts) =>
          if (topic.closed) fuccess(BadRequest("This topic is closed"))
          else if (topic.isOld) fuccess(BadRequest("This topic is archived"))
          else
            categ.team.?? { env.team.cached.isLeader(_, me.id) } flatMap { inOwnTeam =>
              forms
                .post(me, inOwnTeam)
                .bindFromRequest()
                .fold(
                  err =>
                    CategGrantWrite(categId, tryingToPostAsMod = true) {
                      for {
                        captcha     <- forms.anyCaptcha
                        unsub       <- env.timeline.status(s"forum:${topic.id}")(me.id)
                        canModCateg <- access.isGrantedMod(categ.slug)
                      } yield BadRequest(
                        html.forum.topic
                          .show(categ, topic, posts, Some(err -> captcha), unsub, canModCateg = canModCateg)
                      )
                    },
                  data =>
                    CategGrantWrite(categId, tryingToPostAsMod = ~data.modIcon) {
                      CreateRateLimit(ctx.ip) {
                        postApi.makePost(categ, topic, data, me) map { post =>
                          Redirect(routes.ForumPost.redirect(post.id))
                        }
                      }(rateLimitedFu)
                    }
                )
            }
        }
      }
    }

  def edit(postId: ForumPostId) =
    AuthBody { implicit ctx => me =>
      given play.api.mvc.Request[?] = ctx.body
      env.forum.postApi.teamIdOfPostId(postId) flatMap { teamId =>
        teamId.?? { env.team.cached.isLeader(_, me.id) } flatMap { inOwnTeam =>
          postApi getPost postId flatMapz { post =>
            forms
              .postEdit(me, inOwnTeam, post.text)
              .bindFromRequest()
              .fold(
                _ => Redirect(routes.ForumPost.redirect(postId)).toFuccess,
                data =>
                  CreateRateLimit(ctx.ip) {
                    postApi.editPost(postId, data.changes, me).map { post =>
                      Redirect(routes.ForumPost.redirect(post.id))
                    }
                  }(rateLimitedFu)
              )
          }
        }
      }
    }

  def delete(categId: ForumCategId, id: ForumPostId) =
    AuthBody { implicit ctx => me =>
      postApi getPost id flatMapz { post =>
        if (post.userId.exists(_ is me) && !post.erased)
          postApi.erasePost(post) inject Redirect(routes.ForumPost.redirect(id))
        else
          TopicGrantModById(categId, me, post.topicId) {
            env.forum.delete.post(categId, id, me) inject {
              given play.api.mvc.Request[?] = ctx.body
              for {
                userId    <- post.userId
                reasonOpt <- forms.deleteWithReason.bindFromRequest().value
                topic     <- topicRepo.forUser(me.some).byId(post.topicId)
                reason    <- reasonOpt.filter(MsgPreset.forumDeletion.presets.contains)
                preset =
                  if (isGranted(_.ModerateForum)) MsgPreset.forumDeletion.byModerator
                  else if (topic.exists(_ isUblogAuthor me))
                    MsgPreset.forumDeletion.byBlogAuthor(me.username)
                  else MsgPreset.forumDeletion.byTeamLeader(categId)
              } env.msg.api.systemPost(userId, preset(reason))
              NoContent
            }
          }
      }
    }

  def react(categId: ForumCategId, id: ForumPostId, reaction: String, v: Boolean) =
    Auth { implicit ctx => me =>
      CategGrantWrite(categId) {
        postApi.react(categId, id, me, reaction, v) mapz { post =>
          Ok(views.html.forum.post.reactions(post, canReact = true))
        }
      }
    }

  def redirect(id: ForumPostId) =
    Open { implicit ctx =>
      OptionResult(postApi.urlData(id, ctx.me)) { case lila.forum.PostUrlData(categ, topic, page, number) =>
        val call = routes.ForumTopic.show(categ, topic, page)
        Redirect(s"$call#$number").withCanonical(call)
      }
    }
