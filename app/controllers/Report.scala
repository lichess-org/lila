package controllers

import play.api.mvc.AnyContentAsFormUrlEncoded

import views._

import lila.api.{ Context, BodyContext }
import lila.app._
import lila.report.{ Room, Report => ReportModel, Mod => AsMod, Suspect }
import lila.user.{ UserRepo, User => UserModel }

object Report extends LilaController {

  private def env = Env.report
  private def api = env.api

  def list = Secure(_.SeeReport) { implicit ctx => me =>
    renderList(env.modFilters.get(me).fold("all")(_.key))
  }

  def listWithFilter(room: String) = Secure(_.SeeReport) { implicit ctx => me =>
    env.modFilters.set(me, Room(room))
    renderList(room)
  }

  private def renderList(room: String)(implicit ctx: Context) =
    api.unprocessedAndRecentWithFilter(20, Room(room)) zip
      api.countUnprocesssedByRooms flatMap {
        case reports ~ counts =>
          (Env.user.lightUserApi preloadMany reports.flatMap(_.userIds)) inject
            Ok(html.report.list(reports, room, counts))
      }

  def inquiry(id: String) = Secure(_.SeeReport) { implicit ctx => me =>
    for {
      current <- Env.report.api.inquiries ofModId me.id
      newInquiry <- api.inquiries.toggle(AsMod(me), id)
    } yield newInquiry.fold(Redirect(routes.Report.list))(onInquiryStart)
  }

  private def onInquiryStart(inquiry: ReportModel) =
    Redirect(inquiry.room match {
      case Room.Coms => routes.Mod.communicationPrivate(inquiry.user).url
      case _ => routes.User.show(inquiry.user).url + "?mod"
    })

  protected[controllers] def onInquiryClose(inquiry: Option[ReportModel], me: UserModel, force: Boolean = false)(implicit ctx: BodyContext[_]) = {
    def autoNext = ctx.body.body match {
      case AnyContentAsFormUrlEncoded(data) => data.get("next").exists(_.headOption contains "1")
      case _ => false
    }
    inquiry match {
      case None => Redirect(routes.Report.list).fuccess
      case Some(prev) =>
        def redirectToList = Redirect(routes.Report.listWithFilter(prev.room.key))
        if (autoNext) api.next(prev.room) flatMap {
          _.fold(redirectToList.fuccess) { report =>
            api.inquiries.toggle(AsMod(me), report.id) map {
              _.fold(redirectToList)(onInquiryStart)
            }
          }
        }
        else if (force) Redirect(s"${routes.User.show(prev.user)}?mod").fuccess
        else api.inquiries.toggle(AsMod(me), prev.id) map {
          _.fold(redirectToList)(onInquiryStart)
        }
    }
  }

  def process(id: String) = SecureBody(_.SeeReport) { implicit ctx => me =>
    Env.report.api.inquiries ofModId me.id flatMap { inquiry =>
      api.process(AsMod(me), id) >> onInquiryClose(inquiry, me, force = true)
    }
  }

  def xfiles(id: String) = Secure(_.SeeReport) { implicit ctx => me =>
    api.moveToXfiles(id) inject Redirect(routes.Report.list)
  }

  def form = Auth { implicit ctx => implicit me =>
    NotForKids {
      get("username") ?? UserRepo.named flatMap { user =>
        env.forms.createWithCaptcha map {
          case (form, captcha) => Ok(html.report.form(form, user, captcha))
        }
      }
    }
  }

  def create = AuthBody { implicit ctx => implicit me =>
    implicit val req = ctx.body
    env.forms.create.bindFromRequest.fold(
      err => get("username") ?? UserRepo.named flatMap { user =>
        env.forms.anyCaptcha map { captcha =>
          BadRequest(html.report.form(err, user, captcha))
        }
      },
      data => api.create(data, lila.report.Reporter(me)) map { report =>
        Redirect(routes.Report.thanks(data.user.username))
      }
    )
  }

  def thanks(reported: String) = Auth { implicit ctx => me =>
    Env.relation.api.fetchBlocks(me.id, reported) map { blocked =>
      html.report.thanks(reported, blocked)
    }
  }
}
