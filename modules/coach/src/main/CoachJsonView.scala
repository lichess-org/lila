package lila.coach

import monocle.syntax.all.*
import play.api.i18n.Lang
import play.api.libs.json.*
import scalalib.Json.given
import scalalib.paginator.Paginator

import lila.common.Json.{ writeAs, given }
import lila.core.id.ImageId
import lila.core.perf.UserWithPerfs
import lila.core.user.{ FlagCode, FlagName }

final class CoachJsonView(picfitUrl: lila.memo.PicfitUrl):

  private given Writes[ImageId] = writeAs: imageId =>
    picfitUrl.thumbnail(imageId)(Coach.imageDimensions)
  private given OWrites[CoachProfile] = Json.writes
  private given OWrites[Coach.User] = Json.writes
  private given OWrites[Coach] = Json.writes
  private given Writes[UserWithPerfs] = writeAs(_.user.light)

  given OWrites[Coach.WithUser] = Json.writes

  private given OWrites[(FlagCode, FlagName)] = OWrites:
    case (code, name) => Json.obj("code" -> code.value, "name" -> name)

  def pager(
      coaches: Paginator[lila.coach.Coach.WithUser],
      lang: Option[Lang],
      order: lila.coach.CoachPager.Order,
      country: Option[lila.core.user.Flag],
      langCodes: Set[String],
      countries: lila.coach.CountrySelection
  ): JsObject =
    val shortened = coaches.map(_.focus(_.coach.profile).modify(_.shorten))
    Json.toJsObject(shortened) ++ Json.obj(
      "lang" -> lang.fold("all")(_.code),
      "order" -> order.key,
      "country" -> country.fold(lila.coach.allFlags.value)(_.code.value),
      "languages" -> langCodes.toList,
      "countries" -> countries.value
    )
