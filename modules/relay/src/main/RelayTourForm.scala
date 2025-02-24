package lila.relay

import play.api.data.*
import play.api.data.Forms.*
import play.api.data.format.Formatter
import io.mola.galimatias.URL

import lila.common.Form.{ cleanText, cleanNonEmptyText, formatter, into, typeIn, url }
import lila.core.perm.Granter
import lila.core.fide.FideTC
import java.time.ZoneId

final class RelayTourForm(langList: lila.core.i18n.LangList):

  import RelayTourForm.*

  private val spotlightMapping =
    mapping("enabled" -> boolean, "lang" -> langList.popularLanguagesForm.mapping, "title" -> optional(text))(
      RelayTour.Spotlight.apply
    )(unapply)

  private given Formatter[FideTC]            = formatter.stringFormatter(_.toString, FideTC.valueOf)
  private val fideTcMapping: Mapping[FideTC] = typeIn(FideTC.values.toSet)

  private val infoMapping = mapping(
    "format"    -> optional(cleanText(maxLength = 80)),
    "tc"        -> optional(cleanText(maxLength = 80)),
    "fideTc"    -> optional(fideTcMapping),
    "location"  -> optional(cleanText(maxLength = 80)),
    "timeZone"  -> optional(lila.common.Form.timeZone.field),
    "players"   -> optional(cleanText(maxLength = 120)),
    "website"   -> optional(url.field),
    "standings" -> optional(url.field)
  )(RelayTour.Info.apply)(unapply)

  private val pinnedStreamMapping = mapping(
    "name" -> cleanNonEmptyText(maxLength = 100),
    "url" -> url.field
      .verifying("Invalid stream URL", url => RelayPinnedStream("", url, None).upstream.isDefined),
    "text" -> optional(cleanText(maxLength = 100))
  )(RelayPinnedStream.apply)(unapply)

  private given Formatter[RelayTour.Tier] =
    formatter.intOptionFormatter[RelayTour.Tier](_.v, RelayTour.Tier.byV.get)

  val form = Form(
    mapping(
      "name"            -> cleanText(minLength = 3, maxLength = 80).into[RelayTour.Name],
      "info"            -> infoMapping,
      "markdown"        -> optional(cleanText(maxLength = 20_000).into[Markdown]),
      "tier"            -> optional(typeIn(RelayTour.Tier.values.toSet)),
      "showScores"      -> boolean,
      "showRatingDiffs" -> boolean,
      "teamTable"       -> boolean,
      "players" -> optional(
        of(formatter.stringFormatter[RelayPlayersTextarea](_.sortedText, RelayPlayersTextarea(_)))
      ),
      "teams" -> optional(
        of(formatter.stringFormatter[RelayTeamsTextarea](_.sortedText, RelayTeamsTextarea(_)))
      ),
      "spotlight"    -> optional(spotlightMapping),
      "grouping"     -> RelayGroup.form.mapping,
      "pinnedStream" -> optional(pinnedStreamMapping),
      "note"         -> optional(nonEmptyText(maxLength = 20_000))
    )(Data.apply)(unapply)
  ).fill(Data.empty)

  def create = form

  def edit(t: RelayTour.WithGroupTours) = form.fill(Data.make(t))

object RelayTourForm:

  case class Data(
      name: RelayTour.Name,
      info: RelayTour.Info,
      markup: Option[Markdown] = none,
      tier: Option[RelayTour.Tier] = none,
      showScores: Boolean = true,
      showRatingDiffs: Boolean = true,
      teamTable: Boolean = false,
      players: Option[RelayPlayersTextarea] = none,
      teams: Option[RelayTeamsTextarea] = none,
      spotlight: Option[RelayTour.Spotlight] = none,
      grouping: Option[RelayGroup.form.Data] = none,
      pinnedStream: Option[RelayPinnedStream] = none,
      note: Option[String] = none
  ):

    def update(tour: RelayTour)(using me: Me) =
      tour
        .copy(
          name = name,
          info = info,
          markup = markup,
          tier = if Granter(_.Relay) then tier else tour.tier,
          showScores = showScores,
          showRatingDiffs = showRatingDiffs,
          teamTable = teamTable,
          players = players,
          teams = teams,
          spotlight = spotlight.filterNot(_.isEmpty),
          pinnedStream = if Granter(_.StudyAdmin) then pinnedStream else tour.pinnedStream,
          note = note
        )
        .giveOfficialToBroadcasterIf(Granter(_.StudyAdmin))

    def make(using me: Me) =
      RelayTour(
        id = RelayTour.makeId,
        name = name,
        info = info,
        markup = markup,
        ownerIds = NonEmptyList.one(me),
        tier = tier.ifTrue(Granter(_.Relay)),
        active = false,
        live = none,
        createdAt = nowInstant,
        syncedAt = none,
        showScores = showScores,
        showRatingDiffs = showRatingDiffs,
        teamTable = teamTable,
        players = players,
        teams = teams,
        spotlight = spotlight.filterNot(_.isEmpty),
        pinnedStream = pinnedStream,
        note = note
      ).giveOfficialToBroadcasterIf(Granter(_.StudyAdmin))

  object Data:

    val empty = Data(
      RelayTour.Name(""),
      RelayTour.Info(none, none, none, none, ZoneId.systemDefault.some, none, none, none)
    )

    def make(tg: RelayTour.WithGroupTours) =
      import tg.*
      Data(
        name = tour.name,
        info = tour.info.copy(
          fideTc = tour.info.fideTcOrGuess.some,
          timeZone = tour.info.timeZoneOrDefault.some
        ),
        markup = tour.markup,
        tier = tour.tier,
        showScores = tour.showScores,
        showRatingDiffs = tour.showRatingDiffs,
        teamTable = tour.teamTable,
        players = tour.players,
        teams = tour.teams,
        spotlight = tour.spotlight,
        grouping = group.map(RelayGroup.form.Data.apply),
        pinnedStream = tour.pinnedStream,
        note = tour.note
      )
