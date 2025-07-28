package lila.relay

import play.api.data.*
import play.api.data.Forms.*
import play.api.data.format.Formatter
import io.mola.galimatias.URL
import java.time.ZoneId

import lila.common.Form.{ cleanText, cleanNonEmptyText, formatter, into, typeIn, url }
import lila.core.perm.Granter
import lila.core.fide.FideTC
import lila.core.study.Visibility
import chess.tiebreak.{ Tiebreak, CutModifier, LimitModifier }

final class RelayTourForm(langList: lila.core.i18n.LangList, groupForm: RelayGroupForm):

  import RelayTourForm.*

  private val spotlightMapping =
    mapping("enabled" -> boolean, "lang" -> langList.popularLanguagesForm.mapping, "title" -> optional(text))(
      RelayTour.Spotlight.apply
    )(unapply)

  private given Formatter[FideTC] = formatter.stringFormatter(_.toString, FideTC.valueOf)
  private val fideTcMapping: Mapping[FideTC] = typeIn(FideTC.values.toSet)

  private val infoMapping = mapping(
    "format" -> optional(cleanText(maxLength = 80)),
    "tc" -> optional(cleanText(maxLength = 80)),
    "fideTc" -> optional(fideTcMapping),
    "location" -> optional(cleanText(maxLength = 80)),
    "timeZone" -> optional(lila.common.Form.timeZone.field),
    "players" -> optional(cleanText(maxLength = 120)),
    "website" -> optional(url.field),
    "standings" -> optional(url.field)
  )(RelayTour.Info.apply)(unapply)

  private val pinnedStreamMapping = mapping(
    "name" -> cleanNonEmptyText(maxLength = 100),
    "url" -> url.field
      .verifying("Invalid stream URL", url => RelayPinnedStream("", url, None).upstream.isDefined),
    "text" -> optional(cleanText(maxLength = 100))
  )(RelayPinnedStream.apply)(unapply)

  private given Formatter[Visibility] =
    formatter.stringOptionFormatter[Visibility](_.key, Visibility.byKey.get)
  private given Formatter[RelayTour.Tier] =
    formatter.intOptionFormatter[RelayTour.Tier](_.v, RelayTour.Tier.byV.get)

  private given Formatter[Tiebreak] =
    formatter.stringOptionFormatter(_.extendedCode, Tiebreak.preset.mapBy(_.extendedCode).get)

  private val tiebreaksMapping: Mapping[List[Tiebreak]] = list(optional(typeIn(Tiebreak.preset.toSet)))
    .transform[List[Tiebreak]](_.flatten, _.map(some))
    .verifying("Too many tiebreaks", _.sizeIs <= 5)

  val form = Form(
    mapping(
      "name" -> cleanText(minLength = 3, maxLength = 80).into[RelayTour.Name],
      "info" -> infoMapping,
      "markdown" -> optional(cleanText(maxLength = 20_000).into[Markdown]),
      "visibility" -> optional(typeIn(Visibility.values.toSet)),
      "tier" -> optional(typeIn(RelayTour.Tier.values.toSet)),
      "showScores" -> boolean,
      "showRatingDiffs" -> boolean,
      "tiebreaks" -> optional(tiebreaksMapping),
      "teamTable" -> boolean,
      "players" -> optional(
        of(using formatter.stringFormatter[RelayPlayersTextarea](_.sortedText, RelayPlayersTextarea(_)))
      ),
      "teams" -> optional(
        of(using formatter.stringFormatter[RelayTeamsTextarea](_.sortedText, RelayTeamsTextarea(_)))
      ),
      "spotlight" -> optional(spotlightMapping),
      "grouping" -> groupForm.mapping,
      "pinnedStream" -> optional(pinnedStreamMapping),
      "note" -> optional(nonEmptyText(maxLength = 20_000))
    )(Data.apply)(unapply)
  ).fill(Data.empty)

  def create = form

  def edit(t: RelayTour.WithGroupTours) = form.fill(makeData(t))

  private def makeData(tg: RelayTour.WithGroupTours) =
    import tg.*
    Data(
      name = tour.name,
      info = tour.info.copy(
        fideTc = tour.info.fideTcOrGuess.some,
        timeZone = tour.info.timeZoneOrDefault.some
      ),
      markup = tour.markup,
      visibility = tour.visibility.some,
      tier = tour.tier,
      showScores = tour.showScores,
      showRatingDiffs = tour.showRatingDiffs,
      tiebreaks = tour.tiebreaks,
      teamTable = tour.teamTable,
      players = tour.players,
      teams = tour.teams,
      spotlight = tour.spotlight,
      grouping = group.map(groupForm.data),
      pinnedStream = tour.pinnedStream,
      note = tour.note
    )

object RelayTourForm:

  case class Data(
      name: RelayTour.Name,
      info: RelayTour.Info,
      markup: Option[Markdown] = none,
      visibility: Option[Visibility] = none,
      tier: Option[RelayTour.Tier] = none,
      showScores: Boolean = true,
      showRatingDiffs: Boolean = true,
      tiebreaks: Option[List[Tiebreak]] = none,
      teamTable: Boolean = false,
      players: Option[RelayPlayersTextarea] = none,
      teams: Option[RelayTeamsTextarea] = none,
      spotlight: Option[RelayTour.Spotlight] = none,
      grouping: Option[RelayGroupData] = none,
      pinnedStream: Option[RelayPinnedStream] = none,
      note: Option[String] = none
  ):

    def update(tour: RelayTour)(using me: Me) =
      tour
        .copy(
          name = name,
          info = info,
          markup = markup,
          visibility = visibility.ifTrue(!tour.official || Granter(_.Relay)) | tour.visibility,
          tier = if Granter(_.Relay) then tier else tour.tier,
          showScores = showScores,
          showRatingDiffs = showRatingDiffs,
          tiebreaks = tiebreaks,
          teamTable = teamTable,
          players = players,
          teams = teams,
          spotlight = if Granter(_.StudyAdmin) then spotlight.filterNot(_.isEmpty) else tour.spotlight,
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
        visibility = visibility | Visibility.public,
        tier = tier.ifTrue(Granter(_.Relay)),
        active = false,
        live = none,
        createdAt = nowInstant,
        syncedAt = none,
        showScores = showScores,
        showRatingDiffs = showRatingDiffs,
        tiebreaks = tiebreaks,
        teamTable = teamTable,
        players = players,
        teams = teams,
        spotlight = spotlight.filterNot(_.isEmpty).ifTrue(Granter(_.StudyAdmin)),
        pinnedStream = pinnedStream.ifTrue(Granter(_.StudyAdmin)),
        note = note
      ).giveOfficialToBroadcasterIf(Granter(_.StudyAdmin))

  object Data:

    val empty = Data(
      RelayTour.Name(""),
      RelayTour.Info(none, none, none, none, ZoneId.systemDefault.some, none, none, none)
    )
