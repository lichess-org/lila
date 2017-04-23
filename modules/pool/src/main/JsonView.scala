package lila.pool

import play.api.libs.json._

object JsonView {

  implicit val poolConfigJsonWriter = OWrites[PoolConfig] { p =>
    Json.obj(
      "id" -> p.id.value,
      "lim" -> p.clock.limitInMinutes,
      "inc" -> p.clock.incrementSeconds,
      "perf" -> p.perfType.name
    )
  }
}
