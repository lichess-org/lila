package views.tutor

import lila.app.UiEnv.helpers

val bits = lila.tutor.ui.TutorBits(helpers)(views.opening.bits.openingUrl)
val perf = lila.tutor.ui.TutorPerfUi(helpers, bits)
val queue = lila.tutor.ui.TutorQueueUi(helpers, bits)
val reports = lila.tutor.ui.TutorReportsUi(helpers, bits)
val report = lila.tutor.ui.TutorReportUi(helpers, bits, perf)
val home = lila.tutor.ui.TutorHomeUi(helpers, bits, queue, reports)
val openingUi = lila.tutor.ui.TutorOpening(helpers, bits, perf)
