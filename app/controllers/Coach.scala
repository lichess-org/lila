package controllers

import play.api.mvc._, Results._

import lila.app._
import lila.coach.{ Coach => CoachModel, CoachForm }
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
    OptionOk(api find username) { coach =>
      html.coach.show(coach)
    }
  }

  def edit = Auth { implicit ctx =>
    me =>
      OptionResult(api find me) { c =>
        NoCache(Ok(html.coach.edit(c, CoachForm edit c.coach)))
      }
  }

  def editApply = AuthBody { implicit ctx =>
    me =>
      OptionFuResult(api find me) { c =>
        implicit val req = ctx.body
        CoachForm.edit(c.coach).bindFromRequest.fold(
          form => fuccess(BadRequest(html.coach.edit(c, form))),
          data => api.update(c, data) inject Redirect(routes.Coach.edit)
        )
      }
  }

  def picture = Auth { implicit ctx =>
    me =>
      OptionResult(api find me) { c =>
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

  def pictureDelete = Auth { implicit ctx =>
    me =>
      OptionFuResult(api find me) { c =>
        api.deletePicture(c) inject Redirect(routes.Coach.edit)
      }
  }
}
