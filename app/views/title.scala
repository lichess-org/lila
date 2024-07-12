package views.title

import lila.app.UiEnv.{ given, * }
import lila.mod.IpRender.RenderIp
import lila.title.TitleRequest

lazy val ui    = lila.title.ui.TitleUi(helpers)(picfitUrl)
lazy val modUi = lila.title.ui.TitleModUi(helpers)(ui, picfitUrl)

object mod:
  def queue(
      reqs: List[TitleRequest],
      scores: lila.report.Room.Scores,
      pending: lila.mod.ui.PendingCounts
  )(using Context) =
    views.report.layout("title", scores, pending):
      modUi.queue(reqs)

  def show(req: TitleRequest, similar: List[TitleRequest], data: ModData)(using Context)(using me: Me) =
    val modZone =
      given RenderIp = data.renderIp
      frag(
        div(cls := "mod-zone mod-zone-full none"),
        views.user.mod.otherUsers(data.mod, data.user, data.logins, appeals = Nil)(
          cls := "mod-zone communication__logins"
        )
      )
    val fide = data.fide.map: player =>
      frag(
        a(href := routes.Fide.show(player.id, player.slug))(
          strong(player.id),
          " ",
          player.title.fold(strong("untitled"))(userTitleTag),
          " ",
          player.name
        ),
        p(player.ratingsStr),
        p("Year of birth: ", player.year.fold("unknown")(_.toString))
      )
    modUi.show(req, data.user, fide, similar, modZone)

  case class ModData(
      mod: Me,
      user: User,
      fide: Option[lila.fide.FidePlayer],
      logins: lila.security.UserLogins.TableData[lila.mod.UserWithModlog],
      renderIp: RenderIp
  )
