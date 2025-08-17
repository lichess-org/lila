package lila.relay

import chess.format.pgn.TagType
import scalalib.cache.ExpireSetMemo
import lila.common.Bus

final class RelayTagManualOverride(using Executor):

  private val overrides = ExpireSetMemo[(StudyChapterId, TagType)](6.hours)

  Bus.sub[lila.study.AfterSetTagOnRelayChapter]: a =>
    val tag = a.tag
    if tag.value.isEmpty then overrides.remove((a.chapterId, tag.name))
    else overrides.put((a.chapterId, tag.name))

  def exists(chapterId: StudyChapterId, tagType: TagType): Boolean =
    overrides.get((chapterId, tagType))
