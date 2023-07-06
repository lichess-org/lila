package lila.study

export lila.Lila.{ *, given }

private val logger = lila.log("study")

private type ChapterMap = Map[lila.study.StudyChapterId, lila.study.Chapter]
