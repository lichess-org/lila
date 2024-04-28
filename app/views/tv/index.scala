package views.tv

import play.api.libs.json.Json

import lila.app.templating.Environment.{ *, given }

object index:

  def apply(
      channel: lila.tv.Tv.Channel,
      champions: lila.tv.Tv.Champions,
      pov: Pov,
      data: play.api.libs.json.JsObject,
      cross: Option[lila.game.Crosstable.WithMatchup],
      history: List[Pov]
  )(using PageContext) =
    views.round.bits.layout(
      variant = pov.game.variant,
      title = s"${channel.name} TV: ${playerText(pov.player)} vs ${playerText(pov.opponent)}",
      pageModule = PageModule("round", Json.obj("data" -> data, "i18n" -> views.round.jsI18n(pov.game))).some,
      moreCss = cssTag("tv.single"),
      openGraph = OpenGraph(
        title = s"Watch the best ${channel.name.toLowerCase} games of lichess.org",
        description =
          s"Sit back, relax, and watch the best ${channel.name.toLowerCase} Lichess players compete on Lichess TV",
        url = s"$netBaseUrl${routes.Tv.onChannel(channel.key)}"
      ).some,
      zenable = true,
      robots = true,
      withHrefLangs = lila.ui.LangPath(routes.Tv.index).some
    )(
      main(cls := "round tv-single")(
        st.aside(cls := "round__side")(
          side.meta(pov),
          side.channels(channel, champions, "/tv")
        ),
        views.round.bits.roundAppPreload(pov),
        div(cls := "round__underboard")(
          views.round.bits.crosstable(cross, pov.game),
          div(cls := "tv-history")(
            h2(trans.site.previouslyOnLichessTV()),
            div(cls := "now-playing")(
              history.map { views.game.mini(_) }
            )
          )
        )
      )
    )
