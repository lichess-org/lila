package controllers

import play.api.mvc.*

import lila.api.Context
import lila.app.{ given, * }
import lila.coach.{ Coach as CoachModel, CoachPager, CoachProfileForm }
import views.*
import lila.user.Countries

final class Coach(env: Env) extends LilaController(env):

  private def api = env.coach.api

  def homeLang =
    LangPage(routes.Learn.index)(searchResults("all", CoachPager.Order.Login.key, "all", 1))

  def all(page: Int) = search("all", CoachPager.Order.Login.key, "all", page)

  def search(l: String, o: String, c: String, page: Int) = Open:
    searchResults(l, o, c, page)

  private def searchResults(l: String, o: String, c: String, page: Int)(using Context) =
    pageHit
    val order   = CoachPager.Order(o)
    val lang    = (l != "all") ?? play.api.i18n.Lang.get(l)
    val country = (c != "all") ?? Countries.info(c)
    for
      langCodes    <- env.coach.api.allLanguages
      countryCodes <- env.coach.api.allCountries
      pager        <- env.coach.pager(lang, order, country, page)
    yield Ok(html.coach.index(pager, lang, order, langCodes, countryCodes, country))

  def show(username: UserStr) = Open:
    OptionFuResult(api find username): c =>
      WithVisibleCoach(c):
        for
          stu      <- env.study.api.publicByIds(c.coach.profile.studyIds)
          studies  <- env.study.pager.withChaptersAndLiking(ctx.me, 4)(stu)
          posts    <- env.ublog.api.latestPosts(lila.ublog.UblogBlog.Id.User(c.user.id), 4)
          reviews  <- api.reviews.approvedByCoach(c.coach)
          myReview <- ctx.me.?? { api.reviews.find(_, c.coach) }
        yield
          lila.mon.coach.pageView.profile(c.coach.id.value).increment()
          Ok(html.coach.show(c, reviews, studies, posts, myReview))

  def review(username: UserStr) = AuthBody { ctx ?=> me =>
    OptionFuResult(api find username) { c =>
      NoBot:
        WithVisibleCoach(c):
          lila.coach.CoachReviewForm.form
            .bindFromRequest()
            .fold(
              _ => Redirect(routes.Coach.show(c.user.username)).toFuccess,
              data => {
                if (data.score < 4 && !me.marks.reportban)
                  env.report.api.create(
                    lila.report.Report.Candidate(
                      reporter = lila.report.Reporter(me),
                      suspect = lila.report.Suspect(c.user),
                      reason = lila.report.Reason.Other,
                      text = s"[COACH REVIEW rating=${data.score}/5] ${data.text}"
                    )
                  )
                api.reviews.add(me, c.coach, data) inject
                  Redirect(routes.Coach.show(c.user.username))
              }
            )
    }
  }

  def approveReview(id: String) = SecureBody(_.Coach) { ctx ?=> me =>
    OptionFuResult(api.reviews.byId(id)): review =>
      api.byId(review.coachId).dmap(_.exists(_ is me.user)) flatMap {
        if _ then api.reviews.approve(review, getBool("v")) inject Ok
        else notFound
      }
  }

  def modReview(id: String) = SecureBody(_.DisapproveCoachReview) { ctx ?=> me =>
    OptionFuResult(api.reviews byId id): review =>
      env.mod.logApi.coachReview(me.id into ModId, review.coachId.userId, review.userId) >>
        api.reviews.mod(review) inject Redirect(routes.Coach.show(review.coachId.value))
  }

  private def WithVisibleCoach(c: CoachModel.WithUser)(f: Fu[Result])(using ctx: Context) =
    if c.isListed || ctx.me.exists(_ is c.coach) || isGranted(_.Admin) then f
    else notFound

  def edit = Secure(_.Coach) { ctx ?=> me =>
    OptionFuResult(api findOrInit me): c =>
      env.msg.twoFactorReminder(me.id) >>
        api.reviews.pendingByCoach(c.coach) map { reviews =>
          Ok(html.coach.edit(c, CoachProfileForm edit c.coach, reviews)).noCache
        }
  }

  def editApply = SecureBody(_.Coach) { ctx ?=> me =>
    OptionFuResult(api findOrInit me): c =>
      CoachProfileForm
        .edit(c.coach)
        .bindFromRequest()
        .fold(
          _ => fuccess(BadRequest),
          data => api.update(c, data) inject Ok
        )
  }

  def picture = Secure(_.Coach) { ctx ?=> me =>
    OptionResult(api findOrInit me): c =>
      Ok(html.coach.picture(c)).noCache
  }

  def pictureApply = SecureBody(parse.multipartFormData)(lila.security.Permission.Coach) { ctx ?=> me =>
    OptionFuResult(api findOrInit me): c =>
      ctx.body.body.file("picture") match
        case Some(pic) =>
          api.uploadPicture(c, pic) recover { case e: lila.base.LilaException =>
            BadRequest(html.coach.picture(c, e.message.some))
          } inject Redirect(routes.Coach.edit)
        case None => fuccess(Redirect(routes.Coach.edit))
  }
