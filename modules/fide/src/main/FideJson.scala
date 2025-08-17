package lila.fide

import play.api.libs.json.*
import lila.common.Json.given

object FideJson:

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
