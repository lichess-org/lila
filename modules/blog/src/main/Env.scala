package lila.blog

import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config) {

  private val PrismicApiUrl = config getString "prismic.api_url"
  private val PrismicCollection = config getString "prismic.collection"

  lazy val api = new BlogApi(
    prismicUrl = PrismicApiUrl,
    collection = PrismicCollection)
}

object Env {

  lazy val current: Env = "[boot] blog" describes new Env(
    config = lila.common.PlayApp loadConfig "blog")
}
