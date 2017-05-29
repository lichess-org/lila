package lila.i18n

import play.api.i18n.Lang

object I18nDb {

  val all: Messages =
    lila.common.Chronometer.syncEffect(lila.i18n.db.Registry.load) { lap =>
      logger.info(s"${lap.millis}ms MessageDb")
    }

  val langs = all.keySet
}
