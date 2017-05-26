package lila

import play.api.i18n.Lang

package object i18n extends PackageObject with WithPlay {

  type Messages = Map[Lang, Map[String, String]]

  private[i18n] def logger = lila.log("i18n")
}
