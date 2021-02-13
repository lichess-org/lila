package controllers

import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.coach.{ Coach => CoachModel, CoachProfileForm, CoachPager }
import views._

final class Coach(env: Env) extends LilaController(env) {

  private def api = env.coach.api

  def all(page: Int) = search("all", CoachPager.Order.Login.key, page)

  def search(l: String, o: String, page: Int) =
    Open { implicit ctx =>
      pageHit
      val order = CoachPager.Order(o)
      val lang  = (l != "all") ?? play.api.i18n.Lang.get(l)
      env.coach.api.allLanguages flatMap { langCodes =>
        env.coach.pager(lang, order, page) map { pager =>
          Ok(html.coach.index(pager, lang, order, langCodes))
        }
      }
    }

  def show(username: String) =
    Open { implicit ctx =>
      OptionFuResult(api find username) { c =>
        WithVisibleCoach(c) {
          env.study.api.publicByIds {
            c.coach.profile.studyIds.map(_.value).map(lila.study.Study.Id.apply)
          } flatMap env.study.pager.withChaptersAndLiking(ctx.me, 4) flatMap { studies =>
            api.reviews.approvedByCoach(c.coach) flatMap { reviews =>
              ctx.me.?? { api.reviews.mine(_, c.coach) } map { myReview =>
                lila.mon.coach.pageView.profile(c.coach.id.value).increment()
                Ok(html.coach.show(c, reviews, studies, myReview))
              }
            }
          }
        }
      }
    }

  def review(username: String) =
    AuthBody { implicit ctx => me =>
      OptionFuResult(api find username) { c =>
        WithVisibleCoach(c) {
          implicit val req = ctx.body
          lila.coach.CoachReviewForm.form
            .bindFromRequest()
            .fold(
              _ => Redirect(routes.Coach.show(c.user.username)).fuccess,
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
    }

  def approveReview(id: String) =
    SecureBody(_.Coach) { implicit ctx => me =>
      OptionFuResult(api.reviews.byId(id)) { review =>
        api.byId(review.coachId).map(_ ?? (_ is me)) flatMap {
          case false => notFound
          case true  => api.reviews.approve(review, getBool("v")) inject Ok
        }
      }
    }

  def modReview(id: String) =
    SecureBody(_.DisapproveCoachReview) { implicit ctx => me =>
      OptionFuResult(api.reviews byId id) { review =>
        env.mod.logApi.coachReview(me.id, review.coachId.value, review.userId) >>
          api.reviews.mod(review) inject Redirect(routes.Coach.show(review.coachId.value))
      }
    }

  private def WithVisibleCoach(c: CoachModel.WithUser)(f: Fu[Result])(implicit ctx: Context) =
    if (c.coach.isListed || ctx.me.??(c.coach.is) || isGranted(_.Admin)) f
    else notFound

  def edit =
    Secure(_.Coach) { implicit ctx => me =>
      OptionFuResult(api findOrInit me) { c =>
        api.reviews.pendingByCoach(c.coach) map { reviews =>
          NoCache(Ok(html.coach.edit(c, CoachProfileForm edit c.coach, reviews)))
        }
      }
    }

  def editApply =
    SecureBody(_.Coach) { implicit ctx => me =>
      OptionFuResult(api findOrInit me) { c =>
        implicit val req = ctx.body
        CoachProfileForm
          .edit(c.coach)
          .bindFromRequest()
          .fold(
            _ => fuccess(BadRequest),
            data => api.update(c, data) inject Ok
          )
      }
    }

  def picture =
    Secure(_.Coach) { implicit ctx => me =>
      OptionResult(api findOrInit me) { c =>
        NoCache(Ok(html.coach.picture(c)))
      }
    }

  def pictureApply =
    AuthBody(parse.multipartFormData) { implicit ctx => me =>
      OptionFuResult(api findOrInit me) { c =>
        ctx.body.body.file("picture") match {
          case Some(pic) =>
            api.uploadPicture(c, pic) recover {
              case e: lila.base.LilaException => BadRequest(html.coach.picture(c, e.message.some))
            } inject Redirect(routes.Coach.edit())
          case None => fuccess(Redirect(routes.Coach.edit()))
        }
      }
    }

  def pictureDelete =
    Secure(_.Coach) { implicit ctx => me =>
      OptionFuResult(api findOrInit me) { c =>
        api.deletePicture(c) inject Redirect(routes.Coach.edit())
      }
    }
}
