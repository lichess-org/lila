package controllers

import scala.concurrent.duration._
import views._

import lila.app._
import lila.common.IpAddress
import lila.msg.MsgPreset

final class ForumPost(env: Env) extends LilaController(env) with ForumController {

  private val CreateRateLimit =
    new lila.memo.RateLimit[IpAddress](
      credits = 4,
      duration = 5.minutes,
      key = "forum.post",
      enforce = env.net.rateLimit.value
    )

  def search(text: String, page: Int) =
    OpenBody { implicit ctx =>
      NotForKids {
        if (text.trim.isEmpty) Redirect(routes.ForumCateg.index).fuccess
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

  def create(categSlug: String, slug: String, page: Int) =
    AuthBody { implicit ctx => me =>
      NoBot {
        implicit val req = ctx.body
        OptionFuResult(topicApi.show(categSlug, slug, page, ctx.me)) { case (categ, topic, posts) =>
          if (topic.closed) fuccess(BadRequest("This topic is closed"))
          else if (topic.isOld) fuccess(BadRequest("This topic is archived"))
          else
            categ.team.?? { env.team.cached.isLeader(_, me.id) } flatMap { inOwnTeam =>
              forms
                .post(me, inOwnTeam)
                .bindFromRequest()
                .fold(
                  err =>
                    CategGrantWrite(categSlug, tryingToPostAsMod = true) {
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
                    CategGrantWrite(categSlug, tryingToPostAsMod = ~data.modIcon) {
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

  def edit(postId: String) =
    AuthBody { implicit ctx => me =>
      implicit val req = ctx.body
      env.forum.postApi.teamIdOfPostId(postId) flatMap { teamId =>
        teamId.?? { env.team.cached.isLeader(_, me.id) } flatMap { inOwnTeam =>
          postApi getPost postId flatMap {
            _ ?? { post =>
              forms
                .postEdit(me, inOwnTeam, post.text)
                .bindFromRequest()
                .fold(
                  _ => Redirect(routes.ForumPost.redirect(postId)).fuccess,
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
    }

  def delete(categSlug: String, id: String) =
    AuthBody { implicit ctx => me =>
      postApi getPost id flatMap {
        _ ?? { post =>
          if (me.id == ~post.userId && !post.erased)
            postApi.erasePost(post) inject Redirect(routes.ForumPost.redirect(id))
          else
            TopicGrantModById(categSlug, me, post.topicId) {
              env.forum.delete.post(categSlug, id, me) inject {
                implicit val req = ctx.body
                for {
                  userId    <- post.userId
                  reasonOpt <- forms.deleteWithReason.bindFromRequest().value
                  topic     <- topicRepo.forUser(me.some).byId(post.topicId)
                  reason    <- reasonOpt.filter(MsgPreset.forumDeletion.presets.contains)
                  preset =
                    if (isGranted(_.ModerateForum)) MsgPreset.forumDeletion.byModerator
                    else if (topic.exists(_ isUblogAuthor me))
                      MsgPreset.forumDeletion.byBlogAuthor(me.username)
                    else MsgPreset.forumDeletion.byTeamLeader(categSlug)
                } env.msg.api.systemPost(userId, preset(reason))
                NoContent
              }
            }
        }
      }
    }

  def react(categSlug: String, id: String, reaction: String, v: Boolean) =
    Auth { implicit ctx => me =>
      CategGrantWrite(categSlug) {
        postApi.react(categSlug, id, me, reaction, v) map {
          _ ?? { post =>
            Ok(views.html.forum.post.reactions(post, canReact = true))
          }
        }
      }
    }

  def redirect(id: String) =
    Open { implicit ctx =>
      OptionResult(postApi.urlData(id, ctx.me)) { case lila.forum.PostUrlData(categ, topic, page, number) =>
        Redirect(routes.ForumTopic.show(categ, topic, page).url + "#" + number)
      }
    }
}
