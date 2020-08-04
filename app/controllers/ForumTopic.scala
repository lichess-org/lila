package controllers

import scala.concurrent.duration._

import lila.app._
import lila.common.{ HTTPRequest, IpAddress }
import play.api.libs.json._
import views._

final class ForumTopic(env: Env) extends LilaController(env) with ForumController {

  private val CreateRateLimit =
    new lila.memo.RateLimit[IpAddress](2, 5.minutes, key = "forum.topic")

  def form(categSlug: String) =
    Auth { implicit ctx => me =>
      NoBot {
        NotForKids {
          OptionFuOk(env.forum.categRepo bySlug categSlug) { categ =>
            forms.anyCaptcha map { html.forum.topic.form(categ, forms.topic(me), _) }
          }
        }
      }
    }

  def create(categSlug: String) =
    AuthBody { implicit ctx => me =>
      NoBot {
        CategGrantWrite(categSlug) {
          implicit val req = ctx.body
          OptionFuResult(env.forum.categRepo bySlug categSlug) { categ =>
            forms
              .topic(me)
              .bindFromRequest()
              .fold(
                err =>
                  forms.anyCaptcha map { captcha =>
                    BadRequest(html.forum.topic.form(categ, err, captcha))
                  },
                data =>
                  CreateRateLimit(HTTPRequest lastRemoteAddress ctx.req) {
                    topicApi.makeTopic(categ, data, me) map { topic =>
                      Redirect(routes.ForumTopic.show(categ.slug, topic.slug, 1))
                    }
                  }(rateLimitedFu)
              )
          }
        }
      }
    }

  def show(categSlug: String, slug: String, page: Int) =
    Open { implicit ctx =>
      NotForKids {
        OptionFuOk(topicApi.show(categSlug, slug, page, ctx.me)) {
          case (categ, topic, posts) =>
            for {
              unsub    <- ctx.userId ?? env.timeline.status(s"forum:${topic.id}")
              canWrite <- isGrantedWrite(categSlug)
              form <- ctx.me.ifTrue(
                !posts.hasNextPage && canWrite && topic.open && !topic.isOld
              ) ?? { me => forms.postWithCaptcha(me) map some }
              canModCateg <- isGrantedMod(categ.slug)
              _           <- env.user.lightUserApi preloadMany posts.currentPageResults.flatMap(_.userId)
            } yield html.forum.topic.show(categ, topic, posts, form, unsub, canModCateg = canModCateg)
        }
      }
    }

  def close(categSlug: String, slug: String) =
    Auth { implicit ctx => me =>
      CategGrantMod(categSlug) {
        OptionFuRedirect(topicApi.show(categSlug, slug, 1, ctx.me)) {
          case (categ, topic, pag) =>
            topicApi.toggleClose(categ, topic, me) inject
              routes.ForumTopic.show(categSlug, slug, pag.nbPages)
        }
      }
    }

  def hide(categSlug: String, slug: String) =
    Secure(_.ModerateForum) { implicit ctx => me =>
      OptionFuRedirect(topicApi.show(categSlug, slug, 1, ctx.me)) {
        case (categ, topic, pag) =>
          topicApi.toggleHide(categ, topic, me) inject
            routes.ForumTopic.show(categSlug, slug, pag.nbPages)
      }
    }

  def sticky(categSlug: String, slug: String) =
    Auth { implicit ctx => me =>
      CategGrantMod(categSlug) {
        OptionFuRedirect(topicApi.show(categSlug, slug, 1, ctx.me)) {
          case (categ, topic, pag) =>
            topicApi.toggleSticky(categ, topic, me) inject
              routes.ForumTopic.show(categSlug, slug, pag.nbPages)
        }
      }
    }

  /**
    * Returns a list of the usernames of people participating in a forum topic conversation
    */
  def participants(topicId: String) =
    Auth { _ => _ =>
      for {
        userIds   <- postApi allUserIds topicId
        usernames <- env.user.repo usernamesByIds userIds
      } yield Ok(Json.toJson(usernames.sortBy(_.toLowerCase)))
    }
}
