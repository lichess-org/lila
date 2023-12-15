package lila.i18n

import play.api.i18n.Lang
import play.api.data.*
import play.api.data.Forms.*

object LangForm:

  val mapping: Mapping[Lang] =
    text.verifying(l => LangList.allChoices.exists(_._1 == l)).transform(Lang(_), _.code)
