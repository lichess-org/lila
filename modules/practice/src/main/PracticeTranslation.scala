package lila.practice
package ui

import lila.ui.*
import lila.core.i18n.I18nKey
import lila.core.study.data.StudyName

class PracticeFragments(helpers: Helpers):
  import helpers.trans.practice as trp

  private def getFragment[N](name: N, map: Map[N, I18nKey]): I18nKey =
    map.getOrElse(name, I18nKey(name.toString))

  def studiesDesc(name: String): I18nKey = getFragment(name, studiesDescMap)
