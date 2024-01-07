package controllers

import play.api.data.Form
import play.api.data.Forms.*
import play.api.libs.json.*

import views.*

import lila.app.{ given, * }
import lila.common.IpAddress
import lila.forum.ForumCateg.diagnosticId

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
        FoundPage(env.forum.categRepo byId categId): categ =>
          categ.team.so(env.team.api.isLeader(_, me)) flatMap { inOwnTeam =>
            forms.anyCaptcha map { html.forum.topic.form(categ, forms.topic(inOwnTeam), _) }
          }
  }

  def create(categId: ForumCategId) = AuthBody { ctx ?=> me ?=>
    NoBot:
      CategGrantWrite(categId):
        Found(env.forum.categRepo byId categId): categ =>
          categ.team.so(env.team.api.isLeader(_, me)) flatMap { inOwnTeam =>
            forms
              .topic(inOwnTeam)
              .bindFromRequest()
              .fold(
                err =>
                  BadRequest.pageAsync:
                    forms.anyCaptcha.map:
                      html.forum.topic.form(categ, err, _)
                ,
                data =>
                  CreateRateLimit(ctx.ip, rateLimited):
                    topicApi.makeTopic(categ, data) map { topic =>
                      Redirect(routes.ForumTopic.show(categ.slug, topic.slug, 1))
                    }
              )
          }
  }

  def show(categId: ForumCategId, slug: String, page: Int) = Open:
    NotForKids:
      Found(topicApi.show(categId, slug, page)): (categ, topic, posts) =>
        if categId == diagnosticId && !ctx.is(UserId(slug)) && !isGrantedOpt(_.ModerateForum)
        then notFound
        else
          for
            unsub       <- ctx.me soUse env.timeline.status(s"forum:${topic.id}")
            canRead     <- access.isGrantedRead(categ.slug)
            canWrite    <- access.isGrantedWrite(categ.slug, tryingToPostAsMod = true)
            canModCateg <- access.isGrantedMod(categ.slug)
            inOwnTeam   <- ~(categ.team, ctx.me).mapN(env.team.api.isLeader(_, _))
            form <- ctx.me
              .filter(_ => canWrite && topic.open && !topic.isOld)
              .soUse: _ ?=>
                forms.postWithCaptcha(inOwnTeam) map some
            _ <- env.user.lightUserApi preloadMany posts.currentPageResults.flatMap(_.post.userId)
            res <-
              if canRead then
                Ok.page(html.forum.topic.show(categ, topic, posts, form, unsub, canModCateg))
                  .map(_.withCanonical(routes.ForumTopic.show(categ.slug, topic.slug, page)))
              else notFound
          yield res

  def close(categId: ForumCategId, slug: String) = Auth { _ ?=> me ?=>
    TopicGrantModBySlug(categId, slug):
      Found(topicApi.show(categId, slug, 1)): (categ, topic, pag) =>
        topicApi.toggleClose(categ, topic) inject
          Redirect(routes.ForumTopic.show(categId, slug, pag.nbPages))
  }

  def sticky(categId: ForumCategId, slug: String) = Auth { _ ?=> me ?=>
    CategGrantMod(categId):
      Found(topicApi.show(categId, slug, 1)): (categ, topic, pag) =>
        topicApi.toggleSticky(categ, topic) inject
          Redirect(routes.ForumTopic.show(categId, slug, pag.nbPages))
  }

  /** Returns a list of the usernames of people participating in a forum topic conversation
    */
  def participants(topicId: ForumTopicId) = Auth { _ ?=> _ ?=>
    for
      userIds   <- postApi allUserIds topicId
      usernames <- env.user.repo usernamesByIds userIds
    yield Ok(Json.toJson(usernames.sortBy(_.toLowerCase)))
  }

  def diagnostic = AuthBody { ctx ?=> me ?=>
    NoBot:
      val slug = me.userId.value
      Form(single("text" -> nonEmptyText)).bindFromRequest().value match
        case None => BadRequest("Invalid form")
        case Some(text) =>
          env.forum.topicRepo.existsByTree(diagnosticId, slug) flatMap:
            case true => showDiagnostic(slug, text)
            case false =>
              FoundPage(env.forum.categRepo byId diagnosticId): categ =>
                forms.anyCaptcha map { html.forum.topic.makeDiagnostic(categ, forms.topic(false), _, text) }
  }

  def clearDiagnostic(slug: String) = Auth { _ ?=> me ?=>
    if slug != me.userId.value && !isGranted(_.ModerateForum) then notFound
    else env.forum.topicApi.removeTopic(diagnosticId, slug) inject Redirect(routes.ForumCateg.index)
  }

  private def showDiagnostic(slug: String, formText: String)(using Context, Me) =
    Found(topicApi.showLastPage(diagnosticId, slug)): (categ, topic, posts) =>
      val lastPage = topicApi.lastPage(topic)
      for
        form <- forms.postWithCaptcha(false) map some
        _    <- env.user.lightUserApi preloadMany posts.currentPageResults.flatMap(_.post.userId)
        res <-
          Ok.page(html.forum.topic.show(categ, topic, posts, form, None, true, formText.some))
            .map(_.withCanonical(s"${routes.ForumTopic.show(categ.slug, slug, lastPage)}#reply"))
      yield res
