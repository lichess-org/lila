package controllers

import cats.syntax.all.*
import play.api.libs.json.*
import views.*

import lila.app.{ given, * }
import lila.common.IpAddress
import lila.user.Holder

final class ForumTopic(env: Env) extends LilaController(env) with ForumController:

  private val CreateRateLimit =
    lila.memo.RateLimit[IpAddress](
      credits = 2,
      duration = 5.minutes,
      key = "forum.topic",
      enforce = env.net.rateLimit.value
    )

  def form(categId: ForumCategId) = Auth { _ ?=> me =>
    NoBot:
      NotForKids:
        OptionFuOk(env.forum.categRepo byId categId): categ =>
          categ.team.?? { env.team.cached.isLeader(_, me.id) } flatMap { inOwnTeam =>
            forms.anyCaptcha map { html.forum.topic.form(categ, forms.topic(me, inOwnTeam), _) }
          }
  }

  def create(categId: ForumCategId) = AuthBody { ctx ?=> me =>
    NoBot:
      CategGrantWrite(categId):
        OptionFuResult(env.forum.categRepo byId categId) { categ =>
          categ.team.?? { env.team.cached.isLeader(_, me.id) } flatMap { inOwnTeam =>
            forms
              .topic(me, inOwnTeam)
              .bindFromRequest()
              .fold(
                err =>
                  forms.anyCaptcha map { captcha =>
                    BadRequest(html.forum.topic.form(categ, err, captcha))
                  },
                data =>
                  CreateRateLimit(ctx.ip, rateLimitedFu):
                    topicApi.makeTopic(categ, data, me) map { topic =>
                      Redirect(routes.ForumTopic.show(categ.slug, topic.slug, 1))
                    }
              )
          }
        }
  }

  def show(categId: ForumCategId, slug: String, page: Int) = Open:
    NotForKids:
      OptionFuResult(topicApi.show(categId, slug, page, ctx.me)) { case (categ, topic, posts) =>
        for
          unsub       <- ctx.userId ?? env.timeline.status(s"forum:${topic.id}")
          canRead     <- access.isGrantedRead(categ.slug)
          canWrite    <- access.isGrantedWrite(categ.slug, tryingToPostAsMod = true)
          canModCateg <- access.isGrantedMod(categ.slug)
          inOwnTeam <- ~(categ.team, ctx.me).mapN { case (teamId, me) =>
            env.team.cached.isLeader(teamId, me.id)
          }
          form <- ctx.me.filter(_ => canWrite && topic.open && !topic.isOld) ?? { me =>
            forms.postWithCaptcha(me, inOwnTeam) map some
          }
          _ <- env.user.lightUserApi preloadMany posts.currentPageResults.flatMap(_.post.userId)
          res <-
            if (canRead)
              Ok(html.forum.topic.show(categ, topic, posts, form, unsub, canModCateg = canModCateg))
                .withCanonical(routes.ForumTopic.show(categ.slug, topic.slug, page))
                .toFuccess
            else notFound
        yield res
      }

  def close(categId: ForumCategId, slug: String) = Auth { _ ?=> me =>
    TopicGrantModBySlug(categId, me, slug):
      OptionFuRedirect(topicApi.show(categId, slug, 1, ctx.me)): (categ, topic, pag) =>
        topicApi.toggleClose(categ, topic, Holder(me)) inject
          routes.ForumTopic.show(categId, slug, pag.nbPages)
  }

  def sticky(categId: ForumCategId, slug: String) = Auth { _ ?=> me =>
    CategGrantMod(categId):
      OptionFuRedirect(topicApi.show(categId, slug, 1, ctx.me)): (categ, topic, pag) =>
        topicApi.toggleSticky(categ, topic, Holder(me)) inject
          routes.ForumTopic.show(categId, slug, pag.nbPages)
  }

  /** Returns a list of the usernames of people participating in a forum topic conversation
    */
  def participants(topicId: ForumTopicId) = Auth { _ ?=> _ =>
    for
      userIds   <- postApi allUserIds topicId
      usernames <- env.user.repo usernamesByIds userIds
    yield Ok(Json.toJson(usernames.sortBy(_.toLowerCase)))
  }
