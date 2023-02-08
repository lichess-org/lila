package lila.study

import reactivemongo.api.ReadPreference

export lila.Lila.{ *, given }

private val logger = lila.log("study")

private type ChapterMap = Map[lila.study.StudyChapterId, lila.study.Chapter]

private val readPref = ReadPreference.primary
