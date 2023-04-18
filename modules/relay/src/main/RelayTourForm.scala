package lila.relay

import play.api.data.*
import play.api.data.Forms.*

import lila.common.Form.{ cleanText, formatter, into }
import lila.security.Granter
import lila.user.User

final class RelayTourForm:

  import RelayTourForm.*

  val form = Form(
    mapping(
      "name"            -> cleanText(minLength = 3, maxLength = 80),
      "description"     -> cleanText(minLength = 3, maxLength = 400),
      "markdown"        -> optional(cleanText(maxLength = 20_000).into[Markdown]),
      "tier"            -> optional(number(min = RelayTour.Tier.NORMAL, max = RelayTour.Tier.BEST)),
      "autoLeaderboard" -> boolean,
      "players"         -> optional(of(formatter.stringFormatter[RelayPlayers](_.text, RelayPlayers.apply)))
    )(Data.apply)(unapply)
  )

  def create = form

  def edit(t: RelayTour) = form fill Data.make(t)

object RelayTourForm:

  case class Data(
      name: String,
      description: String,
      markup: Option[Markdown],
      tier: Option[RelayTour.Tier],
      autoLeaderboard: Boolean,
      players: Option[RelayPlayers]
  ):

    def update(tour: RelayTour, user: User) =
      tour
        .copy(
          name = name,
          description = description,
          markup = markup,
          tier = tier ifTrue Granter(_.Relay)(user),
          autoLeaderboard = autoLeaderboard,
          players = players
        )
        .reAssignIfOfficial

    def make(user: User) =
      RelayTour(
        _id = RelayTour.makeId,
        name = name,
        description = description,
        markup = markup,
        ownerId = user.id,
        tier = tier ifTrue Granter(_.Relay)(user),
        active = false,
        createdAt = nowInstant,
        syncedAt = none,
        autoLeaderboard = autoLeaderboard,
        players = players
      ).reAssignIfOfficial

  object Data:

    def make(tour: RelayTour) =
      Data(
        name = tour.name,
        description = tour.description,
        markup = tour.markup,
        tier = tour.tier,
        autoLeaderboard = tour.autoLeaderboard,
        players = tour.players
      )
