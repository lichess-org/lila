package controllers

import play.api.mvc.*

import lila.app.{ *, given }
import lila.coach.{ Coach as CoachModel, CoachPager, CoachProfileForm, allFlags }
import lila.user.Flags
import lila.core.user.FlagCode

final class Coach(env: Env) extends LilaController(env):

  import env.coach.api

  def homeLang =
    LangPage(routes.Learn.index)(searchResults("all", CoachPager.Order.Login.key, allFlags, 1))

  def all(page: Int) = search("all", CoachPager.Order.Login.key, allFlags, page)

  def search(l: String, o: String, c: FlagCode, page: Int) = Open:
    searchResults(l, o, c, page)

  private def searchResults(l: String, o: String, c: FlagCode, page: Int)(using Context) =
    val order   = CoachPager.Order(o)
    val lang    = (l != "all").so(play.api.i18n.Lang.get(l))
    val country = Flags.info(c)
    for
      langCodes <- env.coach.api.allLanguages
      countries <- env.coach.api.countrySelection
      pager     <- env.coach.pager(lang, order, country, page)
      page      <- renderPage(views.coach.ui.index(pager, lang, order, langCodes, countries, country))
    yield Ok(page)

  def show(username: UserStr) = Open:
    Found(api.find(username)): c =>
      WithVisibleCoach(c):
        for
          stu     <- env.study.api.publicByIds(c.coach.profile.studyIds)
          studies <- env.study.pager.withChaptersAndLiking(4)(stu)
          posts   <- env.ublog.api.latestPosts(lila.ublog.UblogBlog.Id.User(c.user.id), 4)
          page    <- renderPage(views.coach.show(c, studies, posts))
        yield Ok(page)

  private def WithVisibleCoach(c: CoachModel.WithUser)(f: Fu[Result])(using ctx: Context) =
    if c.isListed || ctx.me.exists(_.is(c.coach)) || isGrantedOpt(_.Admin) then f
    else notFound

  def edit = Secure(_.Coach) { ctx ?=> me ?=>
    FoundPage(api.findOrInit): c =>
      env.msg.twoFactorReminder(me).inject(views.coach.edit(c, CoachProfileForm.edit(c.coach)))
    .map(_.hasPersonalData)
  }

  def editApply = SecureBody(_.Coach) { ctx ?=> me ?=>
    Found(api.findOrInit): c =>
      bindForm(CoachProfileForm.edit(c.coach))(
        _ => BadRequest,
        data => api.update(c, data).inject(Ok)
      )
  }

  def pictureApply = SecureBody(parse.multipartFormData)(_.Coach) { ctx ?=> me ?=>
    Found(api.findOrInit): c =>
      ctx.body.body.file("picture") match
        case Some(pic) =>
          api
            .uploadPicture(c, pic)
            .inject(Redirect(routes.Coach.edit))
            .recoverWith:
              case _: lila.core.lilaism.LilaException => Redirect(routes.Coach.edit)
        case None => Redirect(routes.Coach.edit)
  }
