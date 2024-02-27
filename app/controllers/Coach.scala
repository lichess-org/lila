package controllers

import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.coach.{ Coach => CoachModel, CoachPager, CoachProfileForm }
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
          } flatMap env.study.pager.withChaptersAndLiking(ctx.me, 4) map { studies =>
            lila.mon.coach.pageView.profile(c.coach.id.value).increment()
            Ok(html.coach.show(c, studies))
          }
        }
      }
    }

  private def WithVisibleCoach(c: CoachModel.WithUser)(f: Fu[Result])(implicit ctx: Context) =
    if (c.coach.isListed || ctx.me.??(c.coach.is) || isGranted(_.Admin)) f
    else notFound

  def edit =
    Secure(_.Coach) { implicit ctx => me =>
      OptionResult(api findOrInit me) { c =>
        Ok(html.coach.edit(c, CoachProfileForm edit c.coach)).noCache
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
        Ok(html.coach.picture(c)).noCache
      }
    }

  def pictureApply =
    AuthBody(parse.multipartFormData) { implicit ctx => me =>
      OptionFuResult(api findOrInit me) { c =>
        ctx.body.body.file("picture") match {
          case Some(pic) =>
            (api.uploadPicture(c, pic, me) inject Redirect(routes.Coach.edit)) recover {
              case e: lila.base.LilaException =>
                BadRequest(html.coach.picture(c, e.message.some))
            }
          case None => fuccess(Redirect(routes.Coach.edit))
        }
      }
    }

  def pictureDelete =
    Secure(_.Coach) { implicit ctx => me =>
      OptionFuResult(api findOrInit me) { c =>
        api.deletePicture(c) inject Redirect(routes.Coach.edit)
      }
    }
}
