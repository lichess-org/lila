package lila.api

export lila.Lila.{ *, given }

private val logger = lila log "api"

given (using ctx: AnyContext): Option[lila.user.Me] = ctx.me
