package lila.web

import lila.core.config.BaseUrl

final class Sitemap(baseUrl: BaseUrl):

  val paths = List(
    "training",
    "training/themes",
    "study",
    "broadcast"
  )

  def xml = s"""
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <url>
    ${paths.map(path => s"<loc>$baseUrl/$path</loc>").mkString("\n  ")}
  </url>
</urlset>
"""
