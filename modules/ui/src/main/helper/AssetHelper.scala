package lila.ui

import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.config.AssetBaseUrl

trait AssetHelper:

  def assetBaseUrl: AssetBaseUrl

  // bump flairs version if a flair is changed only (not added or removed)
  val flairVersion = "______2"

  def staticAssetUrl(path: String): String = s"$assetBaseUrl/assets/$path"

  def cdnUrl(path: String) = s"$assetBaseUrl$path"

  def flairSrc(flair: Flair): String = staticAssetUrl(s"$flairVersion/flair/img/$flair.webp")
