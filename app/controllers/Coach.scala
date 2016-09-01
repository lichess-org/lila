package controllers

import play.api.mvc._, Results._

import lila.api.Context
import lila.app._
import lila.coach.{ Coach => CoachModel, CoachProfileForm, CoachReviewForm, CoachPager }
import lila.user.{ User => UserModel, UserRepo }
import views._

object Coach extends LilaController {

  private val api = Env.coach.api

  def allDefault(page: Int) = all(CoachPager.Order.Login.key, page)

  def all(o: String, page: Int) = Open { implicit ctx =>
    val order = CoachPager.Order(o)
    Env.coach.pager(order, page) map { pager =>
      Ok(html.coach.index(pager, order))
    }
  }

  def show(username: String) = Open { implicit ctx =>
    OptionFuResult(api find username) { c =>
      WithVisibleCoach(c) {
        Env.study.api.byIds {
          c.coach.profile.studyIds.map(_.value)
        } flatMap Env.study.pager.withChaptersAndLiking(ctx.me) flatMap { studies =>
          api.reviews.approvedByCoach(c.coach) map { reviews =>
            Ok(html.coach.show(c, reviews, studies, reviewApproval = getBool("review")))
          }
        }
      }
    }
  }

  def review(username: String) = AuthBody { implicit ctx =>
    me =>
      OptionFuResult(api find username) { c =>
        WithVisibleCoach(c) {
          implicit val req = ctx.body
          lila.coach.CoachReviewForm.form.bindFromRequest.fold(
            err => Redirect(routes.Coach.show(c.user.username)).fuccess,
            data => api.reviews.add(me, c.coach, data) map { review =>
              Redirect(routes.Coach.show(c.user.username).url + "?review=1")
            })
        }
      }
  }

  def approveReview(id: String) = SecureBody(_.Coach) { implicit ctx =>
    me =>
      OptionFuResult(api.reviews.byId(id)) { review =>
        api.byId(review.coachId).map(_ ?? (_ is me)) flatMap {
          case false => notFound
          case true  => api.reviews.approve(review, getBool("v")) inject Ok
        }
      }
  }

  private def WithVisibleCoach(c: CoachModel.WithUser)(f: Fu[Result])(implicit ctx: Context) =
    if (c.coach.isFullyEnabled || ctx.me.??(c.coach.is) || isGranted(_.PreviewCoach)) f
    else notFound

  def edit = Secure(_.Coach) { implicit ctx =>
    me =>
      OptionFuResult(api findOrInit me) { c =>
        api.reviews.pendingByCoach(c.coach) map { reviews =>
          NoCache(Ok(html.coach.edit(c, CoachProfileForm edit c.coach, reviews)))
        }
      }
  }

  def editApply = SecureBody(_.Coach) { implicit ctx =>
    me =>
      OptionFuResult(api findOrInit me) { c =>
        implicit val req = ctx.body
        CoachProfileForm.edit(c.coach).bindFromRequest.fold(
          form => fuccess(BadRequest),
          data => api.update(c, data) inject Ok
        )
      }
  }

  def picture = Secure(_.Coach) { implicit ctx =>
    me =>
      OptionResult(api findOrInit me) { c =>
        NoCache(Ok(html.coach.picture(c)))
      }
  }

  def pictureApply = AuthBody(BodyParsers.parse.multipartFormData) { implicit ctx =>
    me =>
      OptionFuResult(api find me) { c =>
        implicit val req = ctx.body
        ctx.body.body.file("picture") match {
          case Some(pic) => api.uploadPicture(c, pic) recover {
            case e: lila.common.LilaException => BadRequest(html.coach.picture(c, e.message.some))
          } inject Redirect(routes.Coach.edit)
          case None => fuccess(Redirect(routes.Coach.edit))
        }
      }
  }

  def pictureDelete = Secure(_.Coach) { implicit ctx =>
    me =>
      OptionFuResult(api findOrInit me) { c =>
        api.deletePicture(c) inject Redirect(routes.Coach.edit)
      }
  }
}
