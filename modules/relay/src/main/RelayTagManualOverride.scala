package lila.relay

import chess.format.pgn.TagType
import scalalib.cache.ExpireSetMemo
import lila.common.Bus

final class RelayTagManualOverride(cacheApi: lila.memo.CacheApi)(using Executor):

  private val overrides = ExpireSetMemo[(StudyChapterId, TagType)](6.hours)

  Bus.sub[lila.study.AfterSetTagOnRelayChapter]: a =>
    overrides.put((a.chapterId, a.tag.name))

  def exists(chapterId: StudyChapterId, tagType: TagType): Boolean =
    overrides.get((chapterId, tagType))
