package lila.relay

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import scala.util.chaining._

import lila.common.Form.{ cleanNonEmptyText, cleanText, toMarkdown }
import lila.game.Game
import lila.security.Granter
import lila.study.Study
import lila.user.User
import lila.common.Markdown

final class RelayTourForm {

  import RelayTourForm._

  val form = Form(
    mapping(
      "name"            -> cleanText(minLength = 3, maxLength = 80),
      "description"     -> cleanText(minLength = 3, maxLength = 400),
      "markdown"        -> optional(toMarkdown(cleanText(maxLength = 20_000))),
      "tier"            -> optional(number(min = RelayTour.Tier.NORMAL, max = RelayTour.Tier.BEST)),
      "autoLeaderboard" -> boolean
    )(Data.apply)(Data.unapply)
  )

  def create = form

  def edit(t: RelayTour) = form fill Data.make(t)
}

object RelayTourForm {

  case class Data(
      name: String,
      description: String,
      markup: Option[Markdown],
      tier: Option[RelayTour.Tier],
      autoLeaderboard: Boolean
  ) {

    def update(tour: RelayTour, user: User) =
      tour.copy(
        name = name,
        description = description,
        markup = markup,
        tier = tier ifTrue Granter(_.Relay)(user),
        autoLeaderboard = autoLeaderboard
      )

    def make(user: User) =
      RelayTour(
        _id = RelayTour.makeId,
        name = name,
        description = description,
        markup = markup,
        ownerId = user.id,
        tier = tier ifTrue Granter(_.Relay)(user),
        active = false,
        createdAt = DateTime.now,
        syncedAt = none,
        autoLeaderboard = autoLeaderboard
      )
  }

  object Data {

    def make(tour: RelayTour) =
      Data(
        name = tour.name,
        description = tour.description,
        markup = tour.markup,
        tier = tour.tier,
        autoLeaderboard = tour.autoLeaderboard
      )
  }
}
