package lila.streamer

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val logger = lila.log("streamer")

private val streamerPageActivationRoute =
  routes.Cms.lonePage(lila.core.id.CmsPageKey("streamer-page-activation"))
