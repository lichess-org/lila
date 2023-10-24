package lila.tutor

import play.api.data.*
import play.api.data.validation.Constraints
import play.api.data.Forms.*

final class TutorForm:

  val request = Form:
    mapping(
      "games" -> numberIn(TutorPeriodReport.NbGames.presets),
      "perf"  -> PerfType.mapping
    )
