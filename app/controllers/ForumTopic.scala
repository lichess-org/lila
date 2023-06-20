package controllers

import cats.syntax.all.*
import play.api.libs.json.*
import views.*

import lila.app.{ given, * }
import lila.common.IpAddress

final class ForumTopic(env: Env) extends LilaController(env) with ForumController:

  private val CreateRateLimit =
    lila.memo.RateLimit[IpAddress](
      credits = 2,
      duration = 5.minutes,
      key = "forum.topic",
      enforce = env.net.rateLimit.value
    )

  def form(categId: ForumCategId) = Auth { _ ?=> me ?=>
    NoBot:
      NotForKids:
        OptionFuOk(env.forum.categRepo byId categId): categ =>
          categ.team.so(env.team.cached.isLeader(_, me)) flatMap { inOwnTeam =>
            forms.anyCaptcha map { html.forum.topic.form(categ, forms.topic(inOwnTeam), _) }
          }
  }

  def create(categId: ForumCategId) = AuthBody { ctx ?=> me ?=>
    NoBot:
      CategGrantWrite(categId):
        OptionFuResult(env.forum.categRepo byId categId): categ =>
          categ.team.so(env.team.cached.isLeader(_, me)) flatMap { inOwnTeam =>
            forms
              .topic(inOwnTeam)
              .bindFromRequest()
              .fold(
                err =>
                  forms.anyCaptcha.map: captcha =>
                    BadRequest(html.forum.topic.form(categ, err, captcha)),
                data =>
                  CreateRateLimit(ctx.ip, rateLimitedFu):
                    topicApi.makeTopic(categ, data) map { topic =>
                      Redirect(routes.ForumTopic.show(categ.slug, topic.slug, 1))
                    }
              )
          }
  }

  def show(categId: ForumCategId, slug: String, page: Int) = Open:
    NotForKids:
      OptionFuResult(topicApi.show(categId, slug, page)): (categ, topic, posts) =>
        for
          unsub       <- ctx.me soUse env.timeline.status(s"forum:${topic.id}")
          canRead     <- access.isGrantedRead(categ.slug)
          canWrite    <- access.isGrantedWrite(categ.slug, tryingToPostAsMod = true)
          canModCateg <- access.isGrantedMod(categ.slug)
          inOwnTeam <- ~(categ.team, ctx.me).mapN: (teamId, me) =>
            env.team.cached.isLeader(teamId, me)
          form <- ctx.me
            .filter(_ => canWrite && topic.open && !topic.isOld)
            .soUse: _ ?=>
              forms.postWithCaptcha(inOwnTeam) map some
          _ <- env.user.lightUserApi preloadMany posts.currentPageResults.flatMap(_.post.userId)
          res <-
            if canRead then
              Ok(html.forum.topic.show(categ, topic, posts, form, unsub, canModCateg = canModCateg))
                .withCanonical(routes.ForumTopic.show(categ.slug, topic.slug, page))
                .toFuccess
            else notFound
        yield res

  def close(categId: ForumCategId, slug: String) = Auth { _ ?=> me ?=>
    TopicGrantModBySlug(categId, slug):
      OptionFuRedirect(topicApi.show(categId, slug, 1)): (categ, topic, pag) =>
        topicApi.toggleClose(categ, topic) inject
          routes.ForumTopic.show(categId, slug, pag.nbPages)
  }

  def sticky(categId: ForumCategId, slug: String) = Auth { _ ?=> me ?=>
    CategGrantMod(categId):
      OptionFuRedirect(topicApi.show(categId, slug, 1)): (categ, topic, pag) =>
        topicApi.toggleSticky(categ, topic) inject
          routes.ForumTopic.show(categId, slug, pag.nbPages)
  }

  /** Returns a list of the usernames of people participating in a forum topic conversation
    */
  def participants(topicId: ForumTopicId) = Auth { _ ?=> _ ?=>
    for
      userIds   <- postApi allUserIds topicId
      usernames <- env.user.repo usernamesByIds userIds
    yield Ok(Json.toJson(usernames.sortBy(_.toLowerCase)))
  }
