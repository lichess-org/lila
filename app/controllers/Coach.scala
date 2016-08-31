package controllers

import play.api.mvc._, Results._

import lila.app._
import lila.coach.{ Coach => CoachModel, CoachProfileForm, CoachReviewForm }
import lila.user.{ User => UserModel, UserRepo }
import views._

object Coach extends LilaController {

  private val api = Env.coach.api

  def index = Open { implicit ctx =>
    api.enabledWithUserList map { coaches =>
      Ok(html.coach.index(coaches))
    }
  }

  def show(username: String) = Open { implicit ctx =>
    OptionFuResult(api find username) { c =>
      if (c.coach.isFullyEnabled || ctx.me.??(c.coach.is) || isGranted(_.PreviewCoach))
        Env.study.api.byIds {
          c.coach.profile.studyIds.map(_.value)
        } flatMap Env.study.pager.withChaptersAndLiking(ctx.me) flatMap { studies =>
          api.reviews.approvedByCoach(c.coach) map { reviews =>
            Ok(html.coach.show(c, reviews, studies))
          }
        }
      else notFound
    }
  }

  def edit = Secure(_.Coach) { implicit ctx =>
    me =>
      OptionResult(api findOrInit me) { c =>
        NoCache(Ok(html.coach.edit(c, CoachProfileForm edit c.coach)))
      }
  }

  def editApply = SecureBody(_.Coach) { implicit ctx =>
    me =>
      OptionFuResult(api findOrInit me) { c =>
        implicit val req = ctx.body
        CoachProfileForm.edit(c.coach).bindFromRequest.fold(
          form => fuccess(BadRequest(html.coach.edit(c, form))),
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
