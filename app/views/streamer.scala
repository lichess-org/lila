package views.streamer

import scalalib.paginator.Paginator

import lila.app.templating.Environment.{ *, given }
import lila.streamer.Streamer
import lila.core.perf.{ UserPerfs, UserWithPerfs }
import lila.rating.UserPerfsExt.best6Perfs

lazy val bits = lila.streamer.ui.StreamerBits(helpers)(
  assetUrl,
  picfitUrl.thumbnail(_, Streamer.imageSize, Streamer.imageSize)
)
private lazy val ui = lila.streamer.ui.StreamerUi(helpers, bits)

def show(
    s: Streamer.WithUserAndStream,
    perfs: UserPerfs,
    activities: Vector[lila.activity.ActivityView]
)(using ctx: PageContext) =
  views.base.layout(
    title = s"${s.titleName} streams chess",
    moreCss = cssTag("streamer.show"),
    modules = EsmInit("bits.streamer"),
    openGraph = OpenGraph(
      title = s"${s.titleName} streams chess",
      description =
        shorten(~(s.streamer.headline.map(_.value).orElse(s.streamer.description.map(_.value))), 152),
      url = s"$netBaseUrl${routes.Streamer.show(s.user.username)}",
      `type` = "video",
      image = s.streamer.hasPicture.option(bits.thumbnail.url(s.streamer))
    ).some,
    csp = defaultCsp.finalizeWithTwitch.some
  ):
    ui.show(
      s,
      perfRatings = perfs.best6Perfs.map { showPerfRating(perfs, _) },
      activities = views.activity(UserWithPerfs(s.user, perfs), activities)
    )

def index(
    live: List[Streamer.WithUserAndStream],
    pager: Paginator[Streamer.WithContext],
    requests: Boolean
)(using ctx: PageContext) =
  val title = if requests then "Streamer approval requests" else trans.streamer.lichessStreamers.txt()
  views.base.layout(
    title = title,
    moreCss = cssTag("streamer.list"),
    modules = infiniteScrollEsmInit ++ EsmInit("bits.streamer")
  )(ui.index(live, pager, requests, title))

def create(using PageContext) =
  views.site.message(
    title = trans.streamer.becomeStreamer.txt(),
    icon = Some(Icon.Mic),
    moreCss = cssTag("streamer.form").some
  )(bits.create)

object edit:

  private lazy val ui = lila.streamer.ui.StreamerEdit(helpers, bits)

  import trans.streamer.*

  type ModData = Option[((List[lila.mod.Modlog], List[lila.user.Note]), List[Streamer])]

  def apply(
      s: Streamer.WithUserAndStream,
      form: play.api.data.Form[?],
      modData: Option[((List[lila.mod.Modlog], List[lila.user.Note]), List[Streamer])]
  )(using ctx: PageContext) =
    views.base.layout(
      title = s"${s.user.titleUsername} ${lichessStreamer.txt()}",
      modules = EsmInit("bits.streamer"),
      moreCss = cssTag("streamer.form")
    ):
      val modZone = modData.map:
        case ((log, notes), streamers) =>
          div(cls := "mod_log status")(modLog(log), br, modNotes(notes)) -> streamers
      ui(s, form, modZone)

  private def modLog(log: List[lila.mod.Modlog])(using Context) = frag(
    strong(cls := "text", dataIcon := Icon.CautionTriangle)(
      "Moderation history",
      log.isEmpty.option(": nothing to show.")
    ),
    log.nonEmpty.option:
      ul(
        log.map: e =>
          li(
            userIdLink(e.mod.userId.some, withTitle = false),
            " ",
            b(e.showAction),
            " ",
            e.details,
            " ",
            momentFromNow(e.date)
          )
      )
  )

  private def modNotes(notes: List[lila.user.Note])(using Context) = frag(
    strong(cls := "text", dataIcon := Icon.CautionTriangle)(
      "Moderator notes",
      notes.isEmpty.option(": nothing to show.")
    ),
    notes.nonEmpty.option:
      ul(
        notes.map: note =>
          (isGranted(_.Admin) || !note.dox).option:
            li(
              p(cls := "meta")(userIdLink(note.from.some), " ", momentFromNow(note.date)),
              p(cls := "text")(richText(note.text, expandImg = false))
            )
      )
  )
