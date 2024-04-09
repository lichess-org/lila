package lila.core
package app

// stuff used in app/
// could be moved to another module
// to be compiled in parallel

opaque type LangPath = String
object LangPath extends OpaqueString[LangPath]:
  def apply(call: play.api.mvc.Call): LangPath = LangPath(call.url)
