package controllers

import play.api.mvc.{ AnyContentAsFormUrlEncoded, Result }
import views._

import lila.api.{ BodyContext, Context }
import lila.app._
import lila.common.HTTPRequest
import lila.report.{ Room, Report => ReportModel, Mod => AsMod, Reporter, Suspect }
import lila.user.{ User => UserModel }

final class Report(
    env: Env,
    userC: => User,
    modC: => Mod
) extends LilaController(env) {

  private def api = env.report.api

  def list =
    Secure(_.SeeReport) { implicit ctx => me =>
      if (env.streamer.liveStreamApi.isStreaming(me.id) && !getBool("force"))
        fuccess(Forbidden(html.site.message.streamingMod))
      else renderList(env.report.modFilters.get(me).fold("all")(_.key))
    }

  def listWithFilter(room: String) =
    Secure(_.SeeReport) { implicit ctx => me =>
      env.report.modFilters.set(me, Room(room))
      renderList(room)
    }

  protected[controllers] def getScores =
    api.maxScores zip env.streamer.api.approval.countRequests zip env.appeal.api.countUnread

  private def renderList(room: String)(implicit ctx: Context) =
    api.openAndRecentWithFilter(12, Room(room)) zip
      getScores flatMap { case (reports, scores ~ streamers ~ appeals) =>
        (env.user.lightUserApi preloadMany reports.flatMap(_.report.userIds)) inject
          Ok(html.report.list(reports, room, scores, streamers, appeals))
      }

  def inquiry(id: String) =
    Secure(_.SeeReport) { _ => me =>
      api.inquiries.toggle(AsMod(me), id) flatMap { case (prev, next) =>
        prev.filter(_.isAppeal).map(_.user).??(env.appeal.api.unreadById) inject
          next.fold(
            Redirect {
              if (prev.exists(_.isAppeal)) routes.Appeal.queue()
              else routes.Report.list()
            }
          )(onInquiryStart)
      }
    }

  private def onInquiryStart(inquiry: ReportModel): Result =
    inquiry.room match {
      case Room.Comm => Redirect(routes.Mod.communicationPrivate(inquiry.user))
      case _         => modC.redirect(inquiry.user)
    }

  protected[controllers] def onInquiryClose(
      inquiry: Option[ReportModel],
      me: UserModel,
      goTo: Option[Suspect],
      force: Boolean = false
  )(implicit ctx: BodyContext[_]): Fu[Result] =
    goTo.ifTrue(HTTPRequest isXhr ctx.req) match {
      case Some(suspect) => userC.renderModZoneActions(suspect.user.username)
      case None =>
        inquiry match {
          case None =>
            goTo.fold(Redirect(routes.Report.list()).fuccess) { s =>
              userC.modZoneOrRedirect(s.user.username)
            }
          case Some(prev) =>
            val dataOpt = ctx.body.body match {
              case AnyContentAsFormUrlEncoded(data) => data.some
              case _                                => none
            }
            def thenGoTo =
              dataOpt.flatMap(_ get "then").flatMap(_.headOption) flatMap {
                case "back"    => HTTPRequest referer ctx.req
                case "profile" => modC.userUrl(prev.user, mod = true).some
                case url       => url.some
              }
            thenGoTo match {
              case Some(url) => Redirect(url).fuccess
              case _ =>
                def redirectToList = Redirect(routes.Report.listWithFilter(prev.room.key))
                if (prev.isAppeal) Redirect(routes.Appeal.queue()).fuccess
                else if (dataOpt.flatMap(_ get "next").exists(_.headOption contains "1"))
                  api.inquiries.toggleNext(AsMod(me), prev.room) map {
                    _.fold(redirectToList)(onInquiryStart)
                  }
                else if (force) userC.modZoneOrRedirect(prev.user)
                else
                  api.inquiries.toggle(AsMod(me), prev.id) map { case (prev, next) =>
                    next
                      .fold(
                        if (prev.exists(_.isAppeal)) Redirect(routes.Appeal.queue())
                        else redirectToList
                      )(onInquiryStart)
                  }
            }
        }
    }

  def process(id: String) =
    SecureBody(_.SeeReport) { implicit ctx => me =>
      api byId id flatMap { inquiry =>
        inquiry.filter(_.isAppeal).map(_.user).??(env.appeal.api.readById) >>
          api.process(AsMod(me), id) >>
          onInquiryClose(inquiry, me, none, force = true)
      }
    }

  def xfiles(id: String) =
    Secure(_.SeeReport) { _ => _ =>
      api.moveToXfiles(id) inject Redirect(routes.Report.list())
    }

  def currentCheatInquiry(username: String) =
    Secure(_.Hunter) { implicit ctx => me =>
      OptionFuResult(env.user.repo named username) { user =>
        api.currentCheatReport(lila.report.Suspect(user)) flatMap {
          _ ?? { report =>
            api.inquiries.toggle(lila.report.Mod(me), report.id).void
          } inject modC.redirect(username, mod = true)
        }
      }
    }

  def form =
    Auth { implicit ctx => _ =>
      get("username") ?? env.user.repo.named flatMap { user =>
        env.report.forms.createWithCaptcha map { case (form, captcha) =>
          Ok(html.report.form(form, user, captcha))
        }
      }
    }

  def create =
    AuthBody { implicit ctx => implicit me =>
      implicit val req = ctx.body
      env.report.forms.create
        .bindFromRequest()
        .fold(
          err =>
            get("username") ?? env.user.repo.named flatMap { user =>
              env.report.forms.anyCaptcha map { captcha =>
                BadRequest(html.report.form(err, user, captcha))
              }
            },
          data =>
            if (data.user.id == me.id) notFound
            else
              api.create(data, Reporter(me)) inject
                Redirect(routes.Report.thanks(data.user.name))
        )
    }

  def flag =
    AuthBody { implicit ctx => implicit me =>
      implicit val req = ctx.body
      env.report.forms.flag
        .bindFromRequest()
        .fold(
          _ => BadRequest.fuccess,
          data =>
            env.user.repo named data.username flatMap {
              _ ?? { user =>
                if (user == me) BadRequest.fuccess
                else api.commFlag(Reporter(me), Suspect(user), data.resource, data.text) inject Ok
              }
            }
        )
    }

  def thanks(reported: String) =
    Auth { implicit ctx => me =>
      env.relation.api.fetchBlocks(me.id, reported) map { blocked =>
        html.report.thanks(reported, blocked)
      }
    }
}
