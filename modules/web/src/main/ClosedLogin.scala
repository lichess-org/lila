package lila.web

object ClosedLogin:

  def acceptsPath(req: Req) =
    isAppeal(req) || isStudyExport(req) || isGameExport(req) || isAccount(req)

  private def isAppeal(req: Req) = req.path.startsWith("/appeal")
  private val gameExportRegexes = List(
    "^/@/[\\w-]{2,30}/download$".r,
    "^/(api/games/user|games/export)/[\\w-]{2,30}($|/.+)".r
  )
  private val studyExportRegex = "^/api/study/by/[\\w-]{2,30}/export.pgn$".r

  private def isGameExport(req: Req) = gameExportRegexes.exists(_.matches(req.path))
  private def isStudyExport(req: Req) = studyExportRegex.matches(req.path)
  private def isAccount(req: Req) =
    List("passwd", "delete", "personal-data").exists: accountPath =>
      req.path.startsWith(s"/account/$accountPath")
