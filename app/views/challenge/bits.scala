package views.challenge

import play.api.libs.json.{ JsObject, Json }

import lila.app.templating.Environment.{ *, given }

import lila.challenge.Challenge

object bits:

  def challengeTitle(c: Challenge)(using ctx: Context) =
    val speed = c.clock.map(_.config).fold(chess.Speed.Correspondence.name) { clock =>
      s"${chess.Speed(clock).name} (${clock.show})"
    }
    val variant = c.variant.exotic.so(s" ${c.variant.name}")
    val challenger = c.challengerUser.fold(trans.site.anonymous.txt()): reg =>
      s"${titleNameOrId(reg.id)}${ctx.pref.showRatings.so(s" (${reg.rating.show})")}"
    val players =
      if c.isOpen then "Open challenge"
      else
        c.destUser.fold(s"Challenge from $challenger"): dest =>
          s"$challenger challenges ${titleNameOrId(dest.id)}${ctx.pref.showRatings.so(s" (${dest.rating.show})")}"
    s"$speed$variant ${c.mode.name} Chess • $players"

  def challengeOpenGraph(c: Challenge)(using Context) =
    OpenGraph(
      title = challengeTitle(c),
      url = s"$netBaseUrl${routes.Round.watcher(c.id, chess.White.name).url}",
      description = "Join the challenge or watch the game here."
    )

  def jsModule(c: Challenge, json: JsObject, owner: Boolean, color: Option[chess.Color] = None)(using
      PageContext
  ) =
    PageModule(
      "bits.challengePage",
      Json.obj(
        "socketUrl" -> s"/challenge/${c.id}/socket/v$apiVersion",
        "xhrUrl"    -> routes.Challenge.show(c.id, color.map(_.name)).url,
        "owner"     -> owner,
        "data"      -> json
      )
    )

  def details(c: Challenge, requestedColor: Option[chess.Color])(using ctx: PageContext) =
    div(cls := "details")(
      div(
        cls      := "variant",
        dataIcon := (if c.initialFen.isDefined then Icon.Feather else c.perfType.icon)
      )(
        div(
          variantLink(c.variant, c.perfType, c.initialFen),
          br,
          span(cls := "clock"):
            c.daysPerTurn
              .fold(shortClockName(c.clock.map(_.config))): days =>
                if days.value == 1 then trans.site.oneDay()
                else trans.site.nbDays.pluralSame(days.value)
        )
      ),
      div(cls := "mode")(
        c.open.fold(c.colorChoice.some)(_.colorFor(requestedColor)).map { colorChoice =>
          frag(colorChoice.trans(), br)
        },
        modeName(c.mode)
      )
    )
