package lila.relay

import io.lemonlabs.uri.AbsoluteUrl
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import scala.util.chaining._

import lila.common.Form.{ cleanNonEmptyText, cleanText }
import lila.game.Game
import lila.security.Granter
import lila.study.Study
import lila.user.User

final class RelayTourForm {

  import RelayTourForm._

  val form = Form(
    mapping(
      "name"        -> cleanText(minLength = 3, maxLength = 80),
      "description" -> cleanText(minLength = 3, maxLength = 400),
      "markup"      -> optional(cleanText(maxLength = 20000)),
      "official"    -> optional(boolean)
    )(Data.apply)(Data.unapply)
  )

  def create = form

  def edit(r: RelayTour) = form fill Data.make(r)
}

object RelayTourForm {

  case class Data(
      name: String,
      description: String,
      markup: Option[String],
      official: Option[Boolean]
  ) {

    def update(tour: RelayTour, user: User) =
      tour.copy(
        name = name,
        description = description,
        markup = markup,
        official = ~official && Granter(_.Relay)(user)
      )

    def make(user: User) =
      RelayTour(
        _id = RelayTour.makeId,
        name = name,
        description = description,
        markup = markup,
        ownerId = user.id,
        official = ~official && Granter(_.Relay)(user),
        active = false,
        createdAt = DateTime.now,
        syncedAt = none
      )
  }

  object Data {

    def make(tour: RelayTour) =
      Data(
        name = tour.name,
        description = tour.description,
        markup = tour.markup,
        official = tour.official option true
      )
  }
}
