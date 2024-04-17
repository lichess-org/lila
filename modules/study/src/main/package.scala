package lila.study

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*
export lila.core.study.data.{ StudyName, StudyChapterName }

private val logger = lila.log("study")

private type ChapterMap = Map[lila.study.StudyChapterId, lila.study.Chapter]
