package controllers

import play.api.mvc.*

import lila.app.{ given, * }
import lila.coach.{ Coach as CoachModel, CoachPager, CoachProfileForm }
import views.*
import lila.user.Flags

final class Coach(env: Env) extends LilaController(env):

  private def api = env.coach.api

  def homeLang =
    LangPage(routes.Learn.index)(searchResults("all", CoachPager.Order.Login.key, "all", 1))

  def all(page: Int) = search("all", CoachPager.Order.Login.key, "all", page)

  def search(l: String, o: String, c: String, page: Int) = Open:
    searchResults(l, o, c, page)

  private def searchResults(l: String, o: String, c: String, page: Int)(using Context) =
    val order   = CoachPager.Order(o)
    val lang    = (l != "all") so play.api.i18n.Lang.get(l)
    val country = (c != "all") so Flags.info(c)
    for
      langCodes    <- env.coach.api.allLanguages
      countryCodes <- env.coach.api.allCountries
      pager        <- env.coach.pager(lang, order, country, page)
      page         <- renderPage(html.coach.index(pager, lang, order, langCodes, countryCodes, country))
    yield Ok(page)

  def show(username: UserStr) = Open:
    Found(api find username): c =>
      WithVisibleCoach(c):
        for
          stu     <- env.study.api.publicByIds(c.coach.profile.studyIds)
          studies <- env.study.pager.withChaptersAndLiking(ctx.me, 4)(stu)
          posts   <- env.ublog.api.latestPosts(lila.ublog.UblogBlog.Id.User(c.user.id), 4)
          page    <- renderPage(html.coach.show(c, studies, posts))
          _ = lila.mon.coach.pageView.profile(c.coach.id.value).increment()
        yield Ok(page)

  private def WithVisibleCoach(c: CoachModel.WithUser)(f: Fu[Result])(using ctx: Context) =
    if c.isListed || ctx.me.exists(_ is c.coach) || isGrantedOpt(_.Admin) then f
    else notFound

  def edit = Secure(_.Coach) { ctx ?=> me ?=>
    FoundPage(api.findOrInit): c =>
      env.msg.twoFactorReminder(me) inject html.coach.edit(c, CoachProfileForm edit c.coach)
    .map(_.noCache)
  }

  def editApply = SecureBody(_.Coach) { ctx ?=> me ?=>
    Found(api.findOrInit): c =>
      CoachProfileForm
        .edit(c.coach)
        .bindFromRequest()
        .fold(
          _ => BadRequest,
          data => api.update(c, data) inject Ok
        )
  }

  def picture = Secure(_.Coach) { ctx ?=> me ?=>
    FoundPage(api.findOrInit)(html.coach.picture(_)).map(_.noCache)
  }

  def pictureApply = SecureBody(parse.multipartFormData)(_.Coach) { ctx ?=> me ?=>
    Found(api.findOrInit): c =>
      ctx.body.body.file("picture") match
        case Some(pic) =>
          api.uploadPicture(c, pic) inject Redirect(routes.Coach.edit) recoverWith {
            case e: lila.base.LilaException =>
              BadRequest.page(html.coach.picture(c, e.message.some))
          }
        case None => Redirect(routes.Coach.edit)
  }
