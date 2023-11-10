package views.html.user.show

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.user.{ Trophy, TrophyKind }

import controllers.routes

object otherTrophies:

  def apply(info: lila.app.mashup.UserInfo)(using ctx: PageContext) =
    frag(
      info.trophies.trophies.filter(_.kind.klass.has("fire-trophy")).some.filter(_.nonEmpty) map { trophies =>
        div(cls := "stacked")(
          trophies.sorted.map { trophy =>
            trophy.kind.icon.map { iconChar =>
              a(
                awardCls(trophy),
                href := trophy.anyUrl,
                ariaTitle(s"${trophy.kind.name}")
              )(raw(iconChar))
            }
          }
        )
      },
      info.trophies.shields.map { shield =>
        a(
          cls := "shield-trophy combo-trophy",
          ariaTitle(s"${shield.categ.name} Shield"),
          href := routes.Tournament.shields
        )(shield.categ.icon)
      },
      info.trophies.revolutions.map { revol =>
        a(
          cls := "revol_trophy combo-trophy",
          ariaTitle(s"${revol.variant.name} Revolution"),
          href := routes.Tournament.show(revol.tourId)
        )(revol.iconChar.toString)
      },
      info.trophies.trophies.find(_.kind._id == TrophyKind.zugMiracle).map(zugMiracleTrophy),
      info.trophies.trophies.filter(_.kind.withCustomImage).map { t =>
        a(
          awardCls(t),
          href := t.anyUrl,
          ariaTitle(t.kind.name),
          style := "width: 65px; margin: 0 3px!important;"
        )(
          img(src := assetUrl(s"images/trophy/${t.kind._id}.png"), cssWidth := 65, cssHeight := 80)
        )
      },
      info.trophies.trophies.filter(_.kind.klass.has("icon3d")).sorted.map { trophy =>
        trophy.kind.icon.map { iconChar =>
          a(
            awardCls(trophy),
            href := trophy.anyUrl,
            ariaTitle(trophy.kind.name)
          )(raw(iconChar))
        }
      },
      info.isCoach option
        a(
          href := routes.Coach.show(info.user.username),
          cls  := "trophy award icon3d coach",
          ariaTitle(trans.coach.lichessCoach.txt())
        )(licon.GraduateCap),
      (info.isStreamer && ctx.kid.no) option {
        val streaming = isStreaming(info.user.id)
        views.html.streamer.bits.redirectLink(info.user.username, streaming.some)(
          cls := List(
            "trophy award icon3d streamer" -> true,
            "streaming"                    -> streaming
          ),
          ariaTitle(if streaming then "Live now!" else "Lichess Streamer")
        )(licon.Mic)
      }
    )

  private def awardCls(t: Trophy) = cls := s"trophy award ${t.kind._id} ${~t.kind.klass}"

  private def zugMiracleTrophy(t: Trophy) = frag(
    styleTag("""
.trophy.zugMiracle {
  display: flex;
  align-items: flex-end;
  height: 40px;
  margin: 0 8px!important;
  transition: 2s;
}
.trophy.zugMiracle img { height: 60px; }
@keyframes psyche { 100% { filter: hue-rotate(360deg); } }
.trophy.zugMiracle:hover {
  transform: translateY(-9px);
  animation: psyche 0.3s ease-in-out infinite alternate;
}"""),
    a(awardCls(t), href := t.anyUrl, ariaTitle(t.kind.name))(
      img(src := assetUrl("images/trophy/zug-trophy.png"))
    )
  )
