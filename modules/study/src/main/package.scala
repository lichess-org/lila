package lila.study

export lila.Core.{ *, given }

private val logger = lila.log("study")

private type ChapterMap = Map[lila.study.StudyChapterId, lila.study.Chapter]
