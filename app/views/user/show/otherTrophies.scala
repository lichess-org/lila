package views.html.user.show

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.user.{ Trophy, TrophyKind }
import lidraughts.user.User

import controllers.routes

object otherTrophies {

  def apply(u: User, info: lidraughts.app.mashup.UserInfo)(implicit ctx: Context) = frag(
    info.trophies.filter(_.kind.klass.has("fire-trophy")).some.filter(_.nonEmpty) map { trophies =>
      div(cls := "stacked")(
        trophies.sorted.map { trophy =>
          trophy.kind.icon.map { iconChar =>
            a(
              awardCls(trophy),
              href := trophy.kind.url,
              ariaTitle(s"${trophy.kind.name}")
            )(raw(iconChar))
          }
        }
      )
    },
    /*info.shields.map { shield =>
      a(
        cls := "shield-trophy combo-trophy",
        ariaTitle(s"${shield.categ.name} Shield"),
        href := routes.Tournament.shields
      )(shield.categ.iconChar.toString)
    },*/
    info.revolutions.map { revol =>
      a(
        cls := "revol_trophy combo-trophy",
        ariaTitle(s"${revol.variant.name} Revolution"),
        href := routes.Tournament.show(revol.tourId)
      )(revol.iconChar.toString)
    },
    info.trophies.filter(_.kind.withCustomImage).map { t =>
      a(awardCls(t), href := t.kind.url, ariaTitle(t.kind.name),
        style := "width: 65px; margin: 0 3px!important;")(
          img(src := staticUrl(s"images/trophy/${t.kind._id}.png"), width := 65, height := 80)
        )
    },
    info.trophies.filter(_.kind.klass.has("icon3d")).sorted.map { trophy =>
      trophy.kind.icon.map { iconChar =>
        a(
          awardCls(trophy),
          href := trophy.kind.url,
          ariaTitle(trophy.kind.name)
        )(raw(iconChar))
      }
    },
    /*info.isCoach option
      a(
        href := routes.Coach.show(u.username),
        cls := "trophy award icon3d coach",
        ariaTitle("Lidraughts Coach")
      )(":"),*/
    info.isStreamer option
      a(
        href := routes.Streamer.show(u.username),
        cls := List(
          "trophy award icon3d streamer" -> true,
          "streaming" -> isStreaming(u.id)
        ),
        ariaTitle(if (isStreaming(u.id)) "Live now!" else "Lidraughts Streamer")
      )("")
  )

  private def awardCls(t: Trophy) = cls := s"trophy award ${t.kind._id} ${~t.kind.klass}"
}
