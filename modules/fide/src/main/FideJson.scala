package lila.fide

import play.api.libs.json.*
import lila.common.Json.given
import lila.core.fide.PhotosJson

final class FideJson(picfitUrl: lila.memo.PicfitUrl):

  given photoWrites: OWrites[FidePlayer.PlayerPhoto] = OWrites: p =>
    Json
      .obj(
        "small" -> FidePlayer.PlayerPhoto(picfitUrl, p.id, _.Small),
        "medium" -> FidePlayer.PlayerPhoto(picfitUrl, p.id, _.Medium)
      )
      .add("credit" -> p.credit)

  def photosJson(photos: Map[chess.FideId, FidePlayer.PlayerPhoto]) = PhotosJson:
    JsObject:
      photos.map: (id, photo) =>
        id.value.toString -> photoWrites.writes(photo)

  given OWrites[FidePlayer] = OWrites: p =>
    Json
      .obj(
        "id" -> p.id,
        "name" -> p.name,
        "federation" -> p.fed,
        "year" -> p.year
      )
      .add("title" -> p.title)
      .add("inactive" -> p.inactive)
      .add("standard" -> p.standard)
      .add("rapid" -> p.rapid)
      .add("blitz" -> p.blitz)
      .add("photo" -> p.photo)

  given OWrites[FidePlayer.WithFollow] = OWrites: p =>
    Json.toJsObject(p.player).add("following" -> p.follow)
