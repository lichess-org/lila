package lila.study

import chess.format.UciPath

import lila.tree.Node

extension (e: UciPath)

  @annotation.tailrec
  def isMainline(node: Node): Boolean =
    e.split match
      case None => true
      case Some(id, rest) =>
        node.children.mainlineFirst match
          case None        => false
          case Some(child) => child.id == id && rest.isMainline(child)

  def toDbField =
    if e.isEmpty then s"root.${UciPathDb.rootDbKey}" else s"root.${UciPathDb.encodeDbKey(e)}"

private[study] object UciPathDb:

  // mongodb objects don't support empty keys
  val rootDbKey = "_"

  // mongodb objects don't support '.' and '$' in keys
  def encodeDbKey(path: UciPath): String =
    path.value.replace('.', 144.toChar).replace('$', 145.toChar)

  def decodeDbKey(key: String): UciPath =
    UciPath(key.replace(144.toChar, '.').replace(145.toChar, '$'))

  def isMainline(node: Node, path: UciPath): Boolean =
    path.split.forall: (id, rest) =>
      node.children.first.so: child =>
        child.id == id && isMainline(child, rest)
