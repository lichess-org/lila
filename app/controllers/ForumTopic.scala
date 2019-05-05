package controllers

import scala.concurrent.duration._

import lila.app._
import lila.common.{ HTTPRequest, IpAddress }
import lila.forum.CategRepo
import play.api.libs.json._
import views._

object ForumTopic extends LilaController with ForumController {

  private val CreateRateLimit = new lila.memo.RateLimit[IpAddress](2, 5 minutes,
    name = "forum create topic",
    key = "forum.topic")

  def form(categSlug: String) = Open { implicit ctx =>
    NotForKids {
      OptionFuOk(CategRepo bySlug categSlug) { categ =>
        forms.anyCaptcha map { html.forum.topic.form(categ, forms.topic, _) }
      }
    }
  }

  def create(categSlug: String) = OpenBody { implicit ctx =>
    CategGrantWrite(categSlug) {
      implicit val req = ctx.body
      OptionFuResult(CategRepo bySlug categSlug) { categ =>
        forms.topic.bindFromRequest.fold(
          err => forms.anyCaptcha map { captcha =>
            BadRequest(html.forum.topic.form(categ, err, captcha))
          },
          data => CreateRateLimit(HTTPRequest lastRemoteAddress ctx.req) {
            topicApi.makeTopic(categ, data) map { topic =>
              Redirect(routes.ForumTopic.show(categ.slug, topic.slug, 1))
            }
          }
        )
      }
    }
  }

  def show(categSlug: String, slug: String, page: Int) = Open { implicit ctx =>
    NotForKids {
      OptionFuOk(topicApi.show(categSlug, slug, page, ctx.troll)) {
        case (categ, topic, posts) => for {
          unsub <- ctx.userId ?? Env.timeline.status(s"forum:${topic.id}")
          canWrite <- isGrantedWrite(categSlug)
          form <- (!posts.hasNextPage && canWrite && topic.open && !topic.isOld) ?? forms.postWithCaptcha.map(_.some)
          canModCateg <- isGrantedMod(categ.slug)
          _ <- Env.user.lightUserApi preloadMany posts.currentPageResults.flatMap(_.userId)
        } yield html.forum.topic.show(categ, topic, posts, form, unsub, canModCateg = canModCateg)
      }
    }
  }

  def close(categSlug: String, slug: String) = Auth { implicit ctx => me =>
    CategGrantMod(categSlug) {
      OptionFuRedirect(topicApi.show(categSlug, slug, 1, ctx.troll)) {
        case (categ, topic, pag) => topicApi.toggleClose(categ, topic, me) inject
          routes.ForumTopic.show(categSlug, slug, pag.nbPages)
      }
    }
  }

  def hide(categSlug: String, slug: String) = Secure(_.ModerateForum) { implicit ctx => me =>
    OptionFuRedirect(topicApi.show(categSlug, slug, 1, ctx.troll)) {
      case (categ, topic, pag) => topicApi.toggleHide(categ, topic, me) inject
        routes.ForumTopic.show(categSlug, slug, pag.nbPages)
    }
  }

  def sticky(categSlug: String, slug: String) = Auth { implicit ctx => me =>
    CategGrantMod(categSlug) {
      OptionFuRedirect(topicApi.show(categSlug, slug, 1, ctx.troll)) {
        case (categ, topic, pag) => topicApi.toggleSticky(categ, topic, me) inject
          routes.ForumTopic.show(categSlug, slug, pag.nbPages)
      }
    }
  }

  /**
   * Returns a list of the usernames of people participating in a forum topic conversation
   */
  def participants(topicId: String) = Auth { implicit ctx => _ =>
    for {
      userIds <- postApi userIds topicId
      usernames <- lila.user.UserRepo usernamesByIds userIds
    } yield Ok(Json.toJson(usernames.sortBy(_.toLowerCase)))
  }
}
