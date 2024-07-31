package lila.relay

import play.api.data.*
import play.api.data.Forms.*

import lila.common.Form.{ cleanText, cleanNonEmptyText, formatter, into, numberIn, url }
import lila.core.perm.Granter

final class RelayTourForm(langList: lila.core.i18n.LangList):

  import RelayTourForm.*

  private val spotlightMapping =
    mapping("enabled" -> boolean, "lang" -> langList.popularLanguagesForm.mapping, "title" -> optional(text))(
      RelayTour.Spotlight.apply
    )(unapply)

  private val infoMapping = mapping(
    "format"  -> optional(cleanText(maxLength = 80)),
    "tc"      -> optional(cleanText(maxLength = 80)),
    "players" -> optional(cleanText(maxLength = 120))
  )(RelayTour.Info.apply)(unapply)

  private val pinnedStreamMapping = mapping(
    "name" -> cleanNonEmptyText(maxLength = 100),
    "url"  -> url.field.verifying("Invalid stream URL", url => RelayPinnedStream("", url).upstream.isDefined)
  )(RelayPinnedStream.apply)(unapply)

  val form = Form(
    mapping(
      "name"            -> cleanText(minLength = 3, maxLength = 80).into[RelayTour.Name],
      "info"            -> infoMapping,
      "markdown"        -> optional(cleanText(maxLength = 20_000).into[Markdown]),
      "tier"            -> optional(numberIn(RelayTour.Tier.keys.keySet)),
      "autoLeaderboard" -> boolean,
      "teamTable"       -> boolean,
      "players" -> optional(
        of(formatter.stringFormatter[RelayPlayersTextarea](_.sortedText, RelayPlayersTextarea(_)))
      ),
      "teams" -> optional(
        of(formatter.stringFormatter[RelayTeamsTextarea](_.sortedText, RelayTeamsTextarea(_)))
      ),
      "spotlight"    -> optional(spotlightMapping),
      "grouping"     -> RelayGroup.form.mapping,
      "pinnedStream" -> optional(pinnedStreamMapping)
    )(Data.apply)(unapply)
  )

  def create = form

  def edit(t: RelayTour.WithGroupTours) = form.fill(Data.make(t))

object RelayTourForm:

  case class Data(
      name: RelayTour.Name,
      info: RelayTour.Info,
      markup: Option[Markdown],
      tier: Option[RelayTour.Tier],
      autoLeaderboard: Boolean,
      teamTable: Boolean,
      players: Option[RelayPlayersTextarea],
      teams: Option[RelayTeamsTextarea],
      spotlight: Option[RelayTour.Spotlight],
      grouping: Option[RelayGroup.form.Data],
      pinnedStream: Option[RelayPinnedStream]
  ):

    def update(tour: RelayTour)(using me: Me) =
      tour
        .copy(
          name = name,
          info = info,
          markup = markup,
          tier = if Granter(_.Relay) then tier else tour.tier,
          autoLeaderboard = autoLeaderboard,
          teamTable = teamTable,
          players = players,
          teams = teams,
          spotlight = spotlight.filterNot(_.isEmpty),
          pinnedStream = pinnedStream
        )
        .giveOfficialToBroadcasterIf(Granter(_.StudyAdmin))

    def make(using me: Me) =
      RelayTour(
        id = RelayTour.makeId,
        name = name,
        info = info,
        markup = markup,
        ownerId = me,
        tier = tier.ifTrue(Granter(_.Relay)),
        active = false,
        live = none,
        createdAt = nowInstant,
        syncedAt = none,
        autoLeaderboard = autoLeaderboard,
        teamTable = teamTable,
        players = players,
        teams = teams,
        spotlight = spotlight.filterNot(_.isEmpty),
        pinnedStream = pinnedStream
      ).giveOfficialToBroadcasterIf(Granter(_.StudyAdmin))

  object Data:

    def make(tg: RelayTour.WithGroupTours) =
      import tg.*
      Data(
        name = tour.name,
        info = tour.info,
        markup = tour.markup,
        tier = tour.tier,
        autoLeaderboard = tour.autoLeaderboard,
        teamTable = tour.teamTable,
        players = tour.players,
        teams = tour.teams,
        spotlight = tour.spotlight,
        grouping = group.map(RelayGroup.form.Data.apply),
        pinnedStream = tour.pinnedStream
      )
