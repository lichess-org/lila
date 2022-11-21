package lila.study

import reactivemongo.api.ReadPreference

export lila.Lila.{ *, given }

private val logger = lila.log("study")

private type ChapterMap = Map[lila.study.Chapter.Id, lila.study.Chapter]

private val readPref = ReadPreference.primary
