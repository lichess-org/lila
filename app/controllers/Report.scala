package controllers

import play.api.mvc.AnyContentAsFormUrlEncoded

import views._

import lidraughts.api.{ Context, BodyContext }
import lidraughts.app._
import lidraughts.common.HTTPRequest
import lidraughts.report.{ Room, Report => ReportModel, Mod => AsMod, Reporter, Suspect }
import lidraughts.user.{ UserRepo, User => UserModel }

object Report extends LidraughtsController {

  private def env = Env.report
  private def api = env.api

  def list = Secure(_.SeeReport) { implicit ctx => me =>
    if (Env.streamer.liveStreamApi.isStreaming(me.id) && !getBool("force")) fuccess(Forbidden(html.site.message.streamingMod))
    else renderList(env.modFilters.get(me).fold("all")(_.key))
  }

  def listWithFilter(room: String) = Secure(_.SeeReport) { implicit ctx => me =>
    env.modFilters.set(me, Room(room))
    renderList(room)
  }

  private def renderList(room: String)(implicit ctx: Context) =
    api.openAndRecentWithFilter(12, Room(room)) zip
      api.countOpenByRooms zip
      Env.streamer.api.approval.countRequests flatMap {
        case reports ~ counts ~ streamers =>
          (Env.user.lightUserApi preloadMany reports.flatMap(_.report.userIds)) inject
            Ok(html.report.list(reports, room, counts, streamers))
      }

  def inquiry(id: String) = Secure(_.SeeReport) { implicit ctx => me =>
    for {
      current <- Env.report.api.inquiries ofModId me.id
      newInquiry <- api.inquiries.toggle(AsMod(me), id)
    } yield newInquiry.fold(Redirect(routes.Report.list))(onInquiryStart)
  }

  private def onInquiryStart(inquiry: ReportModel) =
    inquiry.room match {
      case Room.Coms => Redirect(routes.Mod.communicationPrivate(inquiry.user))
      case _ => Mod.redirect(inquiry.user)
    }

  protected[controllers] def onInquiryClose(
    inquiry: Option[ReportModel],
    me: UserModel,
    goTo: Option[Suspect],
    force: Boolean = false
  )(implicit ctx: BodyContext[_]) = {
    goTo.ifTrue(HTTPRequest isXhr ctx.req) match {
      case Some(suspect) => User.renderModZoneActions(suspect.user.username)
      case None =>
        val dataOpt = ctx.body.body match {
          case AnyContentAsFormUrlEncoded(data) => data.some
          case _ => none
        }
        inquiry match {
          case None =>
            goTo.fold(Redirect(routes.Report.list).fuccess) { s =>
              User.modZoneOrRedirect(s.user.username, me)
            }
          case Some(prev) =>
            def thenGoTo = dataOpt.flatMap(_ get "then").flatMap(_.headOption) flatMap {
              case "back" => HTTPRequest referer ctx.req
              case "profile" => Mod.userUrl(prev.user, true).some
              case url => url.some
            }
            thenGoTo match {
              case Some(url) => api.inquiries.toggle(AsMod(me), prev.id) inject Redirect(url)
              case _ =>
                def redirectToList = Redirect(routes.Report.listWithFilter(prev.room.key))
                if (dataOpt.flatMap(_ get "next").exists(_.headOption contains "1"))
                  api.next(prev.room) flatMap {
                    _.fold(redirectToList.fuccess) { report =>
                      api.inquiries.toggle(AsMod(me), report.id) map {
                        _.fold(redirectToList)(onInquiryStart)
                      }
                    }
                  }
                else if (force) User.modZoneOrRedirect(prev.user, me)
                else api.inquiries.toggle(AsMod(me), prev.id) map {
                  _.fold(redirectToList)(onInquiryStart)
                }
            }
        }
    }
  }

  def process(id: String) = SecureBody(_.SeeReport) { implicit ctx => me =>
    Env.report.api.inquiries ofModId me.id flatMap { inquiry =>
      api.process(AsMod(me), id) >> onInquiryClose(inquiry, me, none, force = true)
    }
  }

  def xfiles(id: String) = Secure(_.SeeReport) { implicit ctx => me =>
    api.moveToXfiles(id) inject Redirect(routes.Report.list)
  }

  def currentCheatInquiry(username: String) = Secure(_.Hunter) { implicit ctx => me =>
    OptionFuResult(UserRepo named username) { user =>
      env.api.currentCheatReport(lidraughts.report.Suspect(user)) flatMap {
        _ ?? { report =>
          env.api.inquiries.toggle(lidraughts.report.Mod(me), report.id)
        } inject Mod.redirect(username, true)
      }
    }
  }

  def form = Auth { implicit ctx => implicit me =>
    get("username") ?? UserRepo.named flatMap { user =>
      env.forms.createWithCaptcha map {
        case (form, captcha) => Ok(html.report.form(form, user, captcha))
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
      data =>
        if (data.user == me) notFound
        else api.create(data candidate lidraughts.report.Reporter(me)) map { report =>
          Redirect(routes.Report.thanks(data.user.username))
        }
    )
  }

  def flag = AuthBody { implicit ctx => implicit me =>
    implicit val req = ctx.body
    env.forms.flag.bindFromRequest.fold(
      err => BadRequest.fuccess,
      data => UserRepo named data.username flatMap {
        _ ?? { user =>
          if (user == me) BadRequest.fuccess
          else api.commFlag(Reporter(me), Suspect(user), data.resource, data.text) inject Ok
        }
      }
    )
  }

  def thanks(reported: String) = Auth { implicit ctx => me =>
    Env.relation.api.fetchBlocks(me.id, reported) map { blocked =>
      html.report.thanks(reported, blocked)
    }
  }
}
