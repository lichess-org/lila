package lila.forum

export lila.Lila.{ *, given }

private def teamSlug(id: TeamId) = s"team-$id"
private val logger               = lila.log("forum")
