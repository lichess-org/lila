package lila.web

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

opaque type LangPath = String
object LangPath extends OpaqueString[LangPath]:
  def apply(call: play.api.mvc.Call): LangPath = LangPath(call.url)
