package lila.web

import play.api.mvc.RequestHeader

object ClosedLogin:

  def acceptsPath(req: RequestHeader) =
    isAppeal(req) || isStudyExport(req) || isGameExport(req) || isAccount(req)

  private def isAppeal(req: RequestHeader) = req.path.startsWith("/appeal")
  private def isGameExport(req: RequestHeader) =
    "^/@/[\\w-]{2,30}/download$".r.matches(req.path) ||
      "^/(api/games/user|games/export)/[\\w-]{2,30}($|/.+)".r.matches(req.path)
  private def isStudyExport(req: RequestHeader) =
    "^/api/study/by/[\\w-]{2,30}/export.pgn$".r.matches(req.path)
  private def isAccount(req: RequestHeader) = req.path.startsWith("/account")
