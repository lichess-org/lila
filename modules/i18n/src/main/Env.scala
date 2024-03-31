package lila.i18n

import com.softwaremill.macwire.*

@Module
object Env:

  val jsDump     = JsDump
  val langList   = LangList
  val langPicker = LangPicker
