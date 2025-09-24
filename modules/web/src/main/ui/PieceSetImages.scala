package lila.web
package ui

import scalatags.Text.all.*
import lila.web.ui.AssetFullHelper

final class PieceSetImages(assets: AssetFullHelper):

  private val cache = scala.collection.concurrent.TrieMap.empty[String, String]

  lila.common.Bus.sub[AssetManifestUpdate.type](_ => cache.clear())

  def load(pieceSet: String): Frag = raw:
    cache.getOrElseUpdate(
      pieceSet, {
        val vars =
          for
            (c, color) <- chess.Color.all.map(c => c.letter -> c.name)
            (r, role) <- chess.Role.all.map(r => r.forsythUpper -> r.name)
          yield s"piece/$pieceSet/$c$r.svg" -> s"---$color-$role"
        val css = s"<style>:root{"
          + vars.map { (path, name) => s"$name:url(${assets.assetUrl(path)});" }.mkString
          + "}</style>"
        if vars.exists { (path, _) => assets.manifest.hashed(path).isEmpty }
        then lila.log("layout").error(s"$pieceSet manifest incomplete")
        css
      }
    )
