package lila.analyse
package ui

import lila.ui.*
import ScalatagsTemplate.{ *, given }

final class AnalyseUi(helpers: Helpers)(externalEngineEndpoint: String):
  import helpers.{ *, given }

  def csp: Update[ContentSecurityPolicy] =
    _.withWebAssembly.withExternalEngine(externalEngineEndpoint)
