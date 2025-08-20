package controllers

import play.api.libs.json.*

import lila.app.{ *, given }
import lila.common.Json.given
import lila.core.id.{ ForumCategId, ForumTopicId, ForumTopicSlug }
import lila.forum.ForumCateg.diagnosticId
import lila.forum.ForumTopic.problemReportSlug

final class ForumTopic(env: Env) extends LilaController(env) with ForumController:

  def form(categId: ForumCategId) = Auth { _ ?=> me ?=>
    NoBot:
      NotForKids:
        FoundPage(env.forum.categRepo.byId(categId)): categ =>
          categ.team.so(env.team.api.isLeader(_, me)).map { inOwnTeam =>
            views.forum.topic.form(categ, forms.topic(inOwnTeam), anyCaptcha)
          }
  }

  def create(categId: ForumCategId) = AuthBody { ctx ?=> me ?=>
    NoBot:
      CategGrantWrite(categId):
        Found(env.forum.categRepo.byId(categId)): categ =>
          categ.team.so(env.team.api.isLeader(_, me)).flatMap { inOwnTeam =>
            bindForm(forms.topic(inOwnTeam))(
              err => BadRequest.page(views.forum.topic.form(categ, err, anyCaptcha)),
              data =>
                limit.forumTopic(ctx.ip, rateLimited):
                  topicApi.makeTopic(categ, data).map { topic =>
                    val url = routes.ForumTopic.show(categ.id, topic.slug, 1).url
                    discard { env.report.api.automodComms(data.automodText, url) }
                    Redirect(url)
                  }
            )
          }
  }

  def show(categId: ForumCategId, slug: ForumTopicSlug, page: Int) = Open:
    NotForKids:
      Found(topicApi.show(categId, slug, page)): (categ, topic, posts) =>
        if categId == diagnosticId &&
          !ctx.userId.exists(me => slug.value.startsWith(me.value)) &&
          !isGrantedOpt(_.ModerateForum)
        then notFound
        else
          for
            unsub <- ctx.me.soUse(env.timeline.status(s"forum:${topic.id}"))
            canRead <- access.isGrantedRead(categ.id)
            canWrite <- access.isGrantedWrite(categ.id, tryingToPostAsMod = true)
            canModCateg <- access.isGrantedMod(categ.id)
            replyBlocked <- ctx.me.soUse(access.isReplyBlockedOnUBlog(topic, canModCateg))
            inOwnTeam <- ~(categ.team, ctx.me).mapN(env.team.api.isLeader(_, _))
            form = ctx.me
              .filter(_ => canWrite && topic.open && !topic.isOld && !replyBlocked)
              .soUse: _ ?=>
                forms.postWithCaptcha(inOwnTeam).some
            _ <- env.user.lightUserApi.preloadMany(posts.currentPageResults.flatMap(_.post.userId))
            res <-
              if canRead then
                Ok.page(
                  views.forum.topic.show(categ, topic, posts, form, unsub, canModCateg, None, replyBlocked)
                ).map(_.withCanonical(routes.ForumTopic.show(categ.id, topic.slug, page)))
              else notFound
          yield res

  def close(categId: ForumCategId, slug: ForumTopicSlug) = Auth { _ ?=> me ?=>
    TopicGrantModBySlug(categId, slug):
      Found(topicApi.show(categId, slug, 1)): (categ, topic, pag) =>
        for _ <- topicApi.toggleClose(categ, topic)
        yield Redirect(routes.ForumTopic.show(categId, slug, pag.nbPages))
  }

  def sticky(categId: ForumCategId, slug: ForumTopicSlug) = Auth { _ ?=> me ?=>
    CategGrantMod(categId):
      Found(topicApi.show(categId, slug, 1)): (categ, topic, pag) =>
        topicApi
          .toggleSticky(categ, topic)
          .inject(Redirect(routes.ForumTopic.show(categId, slug, pag.nbPages)))
  }

  /** Returns a list of the usernames of people participating in a forum topic conversation
    */
  def participants(topicId: ForumTopicId) = Auth { _ ?=> _ ?=>
    for
      userIds <- postApi.allUserIds(topicId)
      usernames <- env.user.repo.usernamesByIds(userIds)
    yield Ok(Json.toJson(usernames.sortBy(_.value.toLowerCase)))
  }

  def diagnostic = AuthBody { ctx ?=> me ?=>
    NoBot:
      val slug = problemReportSlug(me.userId)
      bindForm(env.forum.forms.diagnostic)(
        err => jsonFormError(err),
        text =>
          env.forum.topicRepo
            .existsByTree(diagnosticId, slug)
            .flatMap:
              if _ then showDiagnostic(slug, text)
              else
                FoundPage(env.forum.categRepo.byId(diagnosticId)): categ =>
                  views.forum.topic.makeDiagnostic(categ, forms.topic(false), anyCaptcha, text)
      )
  }

  def clearDiagnostic(slug: ForumTopicSlug) = Auth { _ ?=> me ?=>
    if slug != problemReportSlug(me.userId) && !isGranted(_.ModerateForum) then notFound
    else env.forum.topicApi.removeTopic(diagnosticId, slug).inject(Redirect(routes.ForumCateg.index))
  }

  private def showDiagnostic(slug: ForumTopicSlug, formText: String)(using Context, Me) =
    FoundPage(topicApi.showLastPage(diagnosticId, slug)): (categ, topic, posts) =>
      val form = forms.postWithCaptcha(false).some
      views.forum.topic.show(categ, topic, posts, form, None, true, formText.some)
