package lila.api

import play.api.libs.json._

import lila.common.PimpedJson._
import lila.game.{ Game, PerfPicker }

object UserApiGameJson {

  import lila.round.JsonView._

  implicit val writer: Writes[Game] = Writes[Game] { g =>
    Json.obj(
      "id" -> g.id,
      "rated" -> g.rated,
      "variant" -> g.variant,
      "speed" -> g.speed.key,
      "perf" -> PerfPicker.key(g),
      "timestamp" -> g.createdAt.getDate,
      "turns" -> g.turns,
      "status" -> g.status,
      "clock" -> g.clock,
      "correspondence" -> g.correspondenceClock,
      "opening" -> g.opening,
      "players" -> JsObject(g.players map { p =>
        p.color.name -> Json.obj(
          "userId" -> p.userId,
          "name" -> p.name,
          "aiLevel" -> p.aiLevel,
          "rating" -> p.rating,
          "ratingDiff" -> p.ratingDiff
        ).noNull
      }),
      "opening" -> g.opening.map { o =>
        Json.obj("code" -> o.code, "name" -> o.name)
      },
      "winner" -> g.winnerColor.map(_.name)
    ).noNull
  }
}
