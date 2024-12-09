package lila.i18n

import com.softwaremill.macwire.*

@Module
object Env:

  val langList   = LangList
  val langPicker = LangPicker
