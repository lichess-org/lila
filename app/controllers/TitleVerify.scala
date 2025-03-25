package controllers
import lila.app.{ *, given }
import lila.core.id.{ CmsPageKey, TitleRequestId }
import lila.title.TitleRequest

final class TitleVerify(env: Env, cmsC: => Cms, reportC: => report.Report, userC: => User, modC: => Mod)
    extends LilaController(env):

  import env.title.api
  import views.title.ui

  private def inSiteMenu(title: String = "Your title verification")(using Context) =
    views.site.ui.SitePage(title, "title", "page box box-pad force-ltr").css("bits.titleRequest")

  def index = Auth { _ ?=> me ?=>
    cmsC.orCreateOrNotFound(CmsPageKey("title-verify-index")): page =>
      api.getCurrent.flatMap:
        case Some(req) => Redirect(routes.TitleVerify.show(req.id))
        case None      => Ok.async(ui.index(inSiteMenu(page.title), views.cms.pageContent(page)))
  }

  def form = Auth { _ ?=> _ ?=>
    Ok.async(ui.create(inSiteMenu(), env.title.form.create))
  }

  def create = AuthBody { _ ?=> _ ?=>
    bindForm(env.title.form.create)(
      err => BadRequest.async(ui.create(inSiteMenu(), err)),
      data =>
        api
          .create(data)
          .map: req =>
            Redirect(routes.TitleVerify.show(req.id))
    )
  }

  def show(id: TitleRequestId) = Auth { _ ?=> me ?=>
    Found(api.getForMe(id)): req =>
      if req.userId.is(me)
      then
        if req.isRejectedButCanTryAgain
        then api.tryAgain(req).inject(Redirect(routes.TitleVerify.show(id)))
        else Ok.async(ui.edit(inSiteMenu(), env.title.form.edit(req.data), req))
      else
        for
          data    <- getModData(req)
          similar <- api.findSimilar(req)
          page    <- renderPage(views.title.mod.show(req, similar, data))
        yield Ok(page)
  }

  private def getModData(req: TitleRequest)(using Context)(using me: Me) =
    for
      user   <- env.user.api.byId(req.userId).orFail(s"User ${req.userId} not found")
      users  <- env.security.userLogins(user, 100)
      logins <- userC.loginsTableData(user, users, 100)
      fide   <- req.data.fideId.so(env.fide.playerApi.fetch)
    yield views.title.mod.ModData(
      mod = me,
      user = user,
      fide = fide,
      logins = logins,
      renderIp = env.mod.ipRender.apply
    )

  def update(id: TitleRequestId) = AuthBody { _ ?=> me ?=>
    Found(api.getForMe(id)): req =>
      bindForm(env.title.form.create)(
        err => BadRequest.async(ui.edit(inSiteMenu(), err, req)),
        data =>
          api
            .update(req, data)
            .map: req =>
              val redir = Redirect(routes.TitleVerify.show(req.id))
              if req.status == TitleRequest.Status.building then redir
              else redir.flashSuccess
      )
  }

  def cancel(id: TitleRequestId) = Auth { _ ?=> me ?=>
    Found(api.getForMe(id)): req =>
      api
        .delete(req)
        .inject:
          Redirect(routes.TitleVerify.index).flashSuccess
  }

  def image(id: TitleRequestId, tag: String) = AuthBody(parse.multipartFormData) { ctx ?=> me ?=>
    Found(api.getForMe(id)): req =>
      ctx.body.body.file("image") match
        case Some(image) =>
          limit.imageUpload(ctx.ip, rateLimited):
            api.image
              .upload(req, image, tag)
              .inject(Ok)
              .recover { case e: Exception =>
                BadRequest(e.getMessage)
              }
        case None => api.image.delete(req, tag) >> Ok
  }

  def queue = Secure(_.TitleRequest) { ctx ?=> me ?=>
    for
      reqs              <- api.queue(30)
      (scores, pending) <- reportC.getScores
      page              <- renderPage(views.title.mod.queue(reqs, scores, pending))
    yield Ok(page)
  }

  def process(id: TitleRequestId) = SecureBody(_.TitleRequest) { ctx ?=> me ?=>
    Found(api.getForMe(id)): req =>
      bindForm(env.title.form.process)(
        err => Redirect(routes.TitleVerify.show(req.id)).flashFailure(err.toString),
        data =>
          for
            req  <- api.process(req, data)
            _    <- req.approved.so(onApproved(req))
            next <- api.queue(1).map(_.headOption)
          yield Redirect(next.fold(routes.TitleVerify.queue)(r => routes.TitleVerify.show(r.id))).flashSuccess
      )
  }

  private def onApproved(req: TitleRequest)(using Context, Me) =
    for
      user <- env.user.api.byId(req.userId).orFail(s"User ${req.userId} not found")
      _    <- modC.doSetTitle(user.id, req.data.title.some)
      url  = s"${env.net.baseUrl}${routes.TitleVerify.show(req.id)}"
      note = s"Title verified: ${req.data.title}. Public: ${if req.data.public then "Yes" else "No"}. $url"
      _ <- env.user.noteApi.write(user.id, note, modOnly = true, dox = false)
      _ <- req.data.public.so:
        env.user.repo.setRealName(user.id, req.data.realName)
      _ <- req.data.coach.so:
        env.user.repo.addPermission(user.id, lila.core.perm.Permission.Coach)
      _ <- req.data.coach.so:
        env.mailer.automaticEmail.onBecomeCoach(user)
    yield ()
