package lila.web

import play.api.mvc.RequestHeader

object ClosedLogin:

  def acceptsPath(req: RequestHeader) =
    isAppeal(req) || isStudyExport(req) || isGameExport(req) || isAccount(req)

  private def isAppeal(req: RequestHeader) = req.path.startsWith("/appeal")
  private val gameExportRegexes = List(
    "^/@/[\\w-]{2,30}/download$".r,
    "^/(api/games/user|games/export)/[\\w-]{2,30}($|/.+)".r
  )
  private val studyExportRegex = "^/api/study/by/[\\w-]{2,30}/export.pgn$".r

  private def isGameExport(req: RequestHeader) =
    gameExportRegexes.exists(_.matches(req.path))
  private def isStudyExport(req: RequestHeader) = studyExportRegex.matches(req.path)
  private def isAccount(req: RequestHeader) = req.path.startsWith("/account")
