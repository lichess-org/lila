package views.html.user.show

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.Trophy
import Trophy.Kind
import lila.user.User

import controllers.routes

object otherTrophies {

  def apply(u: User, info: lila.app.mashup.UserInfo)(implicit ctx: Context) = frag(
    info.allTrophies.filter(_.kind.klass.has("fire-trophy")).some.filter(_.nonEmpty) map { trophies =>
      div(cls := "stacked")(
        trophies.sorted.map { trophy =>
          trophy.kind.icon.map { iconChar =>
            a(
              awardCls(trophy),
              href := trophy.kind.url,
              title := s"${trophy.kind.name}"
            )(raw(iconChar))
          }
        }
      )
    },
    info.shields.map { shield =>
      a(
        cls := "shield-trophy combo-trophy",
        title := s"${shield.categ.name} Shield",
        href := routes.Tournament.shields
      )(shield.categ.iconChar.toString)
    },
    info.revolutions.map { revol =>
      a(
        cls := "revol_trophy combo-trophy",
        title := s"${revol.variant.name} Revolution",
        href := routes.Tournament.show(revol.tourId)
      )(revol.iconChar.toString)
    },
    info.allTrophies.find(_.kind == Kind.ZugMiracle).map { t =>
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
        a(awardCls(t), href := t.kind.url, title := t.kind.name)(
          img(src := staticUrl("images/trophy/zug-trophy.png"))
        )
      )
    },
    info.allTrophies.filter(t => t.kind == Kind.ZHWC17 || t.kind == Kind.ZHWC18).::: {
      info.allTrophies.filter(t => t.kind == Kind.AtomicWC16 || t.kind == Kind.AtomicWC17 || t.kind == Kind.AtomicWC18)
    }.map { t =>
      a(awardCls(t), href := t.kind.url, title := t.kind.name,
        style := "width: 65px; height: 80px; margin: 0 3px!important;")(
          img(src := staticUrl(s"images/trophy/${t.kind.key}.png"), width := 65, height := 80)
        )
    },
    info.allTrophies.filter(_.kind.klass.has("icon3d")).sorted.map { trophy =>
      trophy.kind.icon.map { iconChar =>
        a(
          awardCls(trophy),
          href := trophy.kind.url,
          title := trophy.kind.name
        )(raw(iconChar))
      }
    },
    info.isCoach option
      a(
        href := routes.Coach.show(u.username),
        cls := "trophy award icon3d coach", title := "Lichess Coach"
      )(":"),
    info.isStreamer option
      a(
        href := routes.Streamer.show(u.username),
        cls := List(
          "trophy award icon3d streamer" -> true,
          "streaming" -> isStreaming(u.id)
        ),
        title := (if (isStreaming(u.id)) "Live now!" else "Lichess Streamer")
      )("")
  )

  private def awardCls(t: Trophy) = cls := s"trophy award ${t.kind.key} ${~t.kind.klass}"
}
