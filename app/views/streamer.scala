package views.streamer

import scalalib.paginator.Paginator

import lila.app.UiEnv.{ *, given }
import lila.core.perf.{ UserPerfs, UserWithPerfs }
import lila.rating.UserPerfsExt.best6Perfs
import lila.streamer.Streamer

lazy val bits       = lila.streamer.ui.StreamerBits(helpers)(picfitUrl)
private lazy val ui = lila.streamer.ui.StreamerUi(helpers, bits)
export ui.index

def show(s: Streamer.WithUserAndStream, perfs: UserPerfs, activities: Seq[lila.activity.ActivityView])(using
    Context
) =
  ui.show(
    s,
    perfRatings = perfs.best6Perfs.map { showPerfRating(perfs, _) },
    activities = views.activity(UserWithPerfs(s.user, perfs), activities)
  )

def create(using Context) =
  views.site
    .message(
      title = trans.streamer.becomeStreamer.txt(),
      icon = Some(Icon.Mic)
    )
    .css("bits.streamer.form")(bits.create)

object edit:

  private lazy val ui = lila.streamer.ui.StreamerEdit(helpers, bits)

  def apply(
      s: Streamer.WithUserAndStream,
      form: play.api.data.Form[?],
      modData: Option[((List[lila.mod.Modlog], List[lila.user.Note]), List[Streamer])]
  )(using ctx: Context) =
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
