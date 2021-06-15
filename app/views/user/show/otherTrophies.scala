package views.html.user.show

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.{ Trophy, TrophyKind }

import controllers.routes

object otherTrophies {

  def apply(info: lila.app.mashup.UserInfo)(implicit ctx: Context) =
    frag(
      info.trophies.filter(_.kind.klass.has("fire-trophy")).some.filter(_.nonEmpty) map { trophies =>
        div(cls := "stacked")(
          trophies.sorted.map { trophy =>
            trophy.kind.icon.map { iconChar =>
              a(
                awardCls(trophy),
                href := trophy.kind.url.orElse(trophy.url),
                ariaTitle(s"${trophy.kind.name}")
              )(raw(iconChar))
            }
          }
        )
      },
      info.shields.map { shield =>
        a(
          cls := "shield-trophy combo-trophy",
          ariaTitle(s"${shield.categ.name} Shield"),
          href := routes.Tournament.shields
        )(shield.categ.iconChar.toString)
      },
      info.revolutions.map { revol =>
        a(
          cls := "revol_trophy combo-trophy",
          ariaTitle(s"${revol.variant.name} Revolution"),
          href := routes.Tournament.show(revol.tourId)
        )(revol.iconChar.toString)
      },
      info.trophies.find(_.kind._id == TrophyKind.zugMiracle).map { t =>
        frag(
          styleTag("""
.trophy.zugMiracle {
  display: flex;
  align-items: flex-end;
  height: 40px;
  margin: 0 8px!important;
  transition: 2s;
}
.trophy.zugMiracle img {
  height: 60px;
}
@keyframes psyche {
 100% { filter: hue-rotate(360deg); }
}
.trophy.zugMiracle:hover {
  transform: translateY(-9px);
  animation: psyche 0.3s ease-in-out infinite alternate;
}"""),
          a(awardCls(t), href := t.kind.url, ariaTitle(t.kind.name))(
            img(src := assetUrl("images/trophy/zug-trophy.png"))
          )
        )
      },
      info.trophies.filter(_.kind.withCustomImage).map { t =>
        a(
          awardCls(t),
          href := t.kind.url,
          ariaTitle(t.kind.name),
          style := "width: 65px; margin: 0 3px!important;"
        )(
          img(src := assetUrl(s"images/trophy/${t.kind._id}.png"), width := 65, height := 80)
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
      info.isCoach option
        a(
          href := routes.Coach.show(info.user.username),
          cls := "trophy award icon3d coach",
          ariaTitle(trans.coach.lichessCoach.txt())
        )(""),
      (info.isStreamer && ctx.noKid) option {
        val streaming = isStreaming(info.user.id)
        views.html.streamer.bits.redirectLink(info.user.username, streaming.some)(
          cls := List(
            "trophy award icon3d streamer" -> true,
            "streaming"                    -> streaming
          ),
          ariaTitle(if (streaming) "Live now!" else "Lichess Streamer")
        )("")
      }
    )

  private def awardCls(t: Trophy) = cls := s"trophy award ${t.kind._id} ${~t.kind.klass}"
}
