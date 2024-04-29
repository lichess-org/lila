package views.opening

import lila.app.templating.Environment.{ *, given }

lazy val bits = lila.opening.ui.OpeningBits(helpers)
lazy val wiki = lila.opening.ui.WikiUi(helpers, bits)
lazy val ui   = lila.opening.ui.OpeningUi(helpers, bits, wiki)
