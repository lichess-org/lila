package lila.core
package html

import lila.core.config.NetDomain
import scalatags.Text.all.Frag

trait HtmlOps:
  def richText(rawText: String, nl2br: Boolean = true, expandImg: Boolean = true)(using NetDomain): Frag
