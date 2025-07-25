/*
 * Copyright 2013 Toshiyuki Takahashi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tototoshi.csv

import scala.annotation.switch

object CSVParser:

  class MalformedCSVException(message: String) extends Exception(message)

  private type State = Int
  final private val Start = 0
  final private val Field = 1
  final private val Delimiter = 2
  final private val End = 3
  final private val QuoteStart = 4
  final private val QuoteEnd = 5
  final private val QuotedField = 6

  def apply(input: String, escapeChar: Char, delimiter: Char, quoteChar: Char): Option[List[String]] =
    val buf: Array[Char] = input.toCharArray
    var fields: Vector[String] = Vector()
    var field = new StringBuilder
    var state: State = Start
    var pos = 0
    val buflen = buf.length

    if buf.length > 0 && buf(0) == '\uFEFF' then pos += 1

    while state != End && pos < buflen do
      val c = buf(pos)
      (state: @switch) match
        case Start =>
          c match
            case `quoteChar` =>
              state = QuoteStart
              pos += 1
            case `delimiter` =>
              fields :+= field.toString
              field = new StringBuilder
              state = Delimiter
              pos += 1
            case '\n' | '\u2028' | '\u2029' | '\u0085' =>
              fields :+= field.toString
              field = new StringBuilder
              state = End
              pos += 1
            case '\r' =>
              if pos + 1 < buflen && buf(1) == '\n' then pos += 1
              fields :+= field.toString
              field = new StringBuilder
              state = End
              pos += 1
            case x =>
              field += x
              state = Field
              pos += 1
        case Delimiter =>
          c match
            case `quoteChar` =>
              state = QuoteStart
              pos += 1
            case `escapeChar` =>
              if pos + 1 < buflen
                && (buf(pos + 1) == escapeChar || buf(pos + 1) == delimiter)
              then
                field += buf(pos + 1)
                state = Field
                pos += 2
              else throw new MalformedCSVException(buf.mkString)
            case `delimiter` =>
              fields :+= field.toString
              field = new StringBuilder
              state = Delimiter
              pos += 1
            case '\n' | '\u2028' | '\u2029' | '\u0085' =>
              fields :+= field.toString
              field = new StringBuilder
              state = End
              pos += 1
            case '\r' =>
              if pos + 1 < buflen && buf(1) == '\n' then pos += 1
              fields :+= field.toString
              field = new StringBuilder
              state = End
              pos += 1
            case x =>
              field += x
              state = Field
              pos += 1
        case Field =>
          c match
            case `escapeChar` =>
              if pos + 1 < buflen then
                if buf(pos + 1) == escapeChar
                  || buf(pos + 1) == delimiter
                then
                  field += buf(pos + 1)
                  state = Field
                  pos += 2
                else throw new MalformedCSVException(buf.mkString)
              else
                state = QuoteEnd
                pos += 1
            case `delimiter` =>
              fields :+= field.toString
              field = new StringBuilder
              state = Delimiter
              pos += 1
            case '\n' | '\u2028' | '\u2029' | '\u0085' =>
              fields :+= field.toString
              field = new StringBuilder
              state = End
              pos += 1
            case '\r' =>
              if pos + 1 < buflen && buf(1) == '\n' then pos += 1
              fields :+= field.toString
              field = new StringBuilder
              state = End
              pos += 1
            case x =>
              field += x
              state = Field
              pos += 1
        case QuoteStart =>
          c match
            case `escapeChar` if escapeChar != quoteChar =>
              if pos + 1 < buflen then
                if buf(pos + 1) == escapeChar
                  || buf(pos + 1) == quoteChar
                then
                  field += buf(pos + 1)
                  state = QuotedField
                  pos += 2
                else throw new MalformedCSVException(buf.mkString)
              else throw new MalformedCSVException(buf.mkString)
            case `quoteChar` =>
              if pos + 1 < buflen && buf(pos + 1) == quoteChar then
                field += quoteChar
                state = QuotedField
                pos += 2
              else
                state = QuoteEnd
                pos += 1
            case x =>
              field += x
              state = QuotedField
              pos += 1
        case QuoteEnd =>
          c match
            case `delimiter` =>
              fields :+= field.toString
              field = new StringBuilder
              state = Delimiter
              pos += 1
            case '\n' | '\u2028' | '\u2029' | '\u0085' =>
              fields :+= field.toString
              field = new StringBuilder
              state = End
              pos += 1
            case '\r' =>
              if pos + 1 < buflen && buf(1) == '\n' then pos += 1
              fields :+= field.toString
              field = new StringBuilder
              state = End
              pos += 1
            case _ =>
              throw new MalformedCSVException(buf.mkString)
        case QuotedField =>
          c match
            case `escapeChar` if escapeChar != quoteChar =>
              if pos + 1 < buflen then
                if buf(pos + 1) == escapeChar
                  || buf(pos + 1) == quoteChar
                then
                  field += buf(pos + 1)
                  state = QuotedField
                  pos += 2
                else
                  field += buf(pos)
                  field += buf(pos + 1)
                  state = QuotedField
                  pos += 2
              else throw new MalformedCSVException(buf.mkString)
            case `quoteChar` =>
              if pos + 1 < buflen && buf(pos + 1) == quoteChar then
                field += quoteChar
                state = QuotedField
                pos += 2
              else
                state = QuoteEnd
                pos += 1
            case x =>
              field += x
              state = QuotedField
              pos += 1
        case End =>
          sys.error("unexpected error")
    (state: @switch) match
      case Delimiter =>
        fields :+= ""
        Some(fields.toList)
      case QuotedField =>
        None
      case _ =>
        // When no crlf at end of file
        state match
          case Field | QuoteEnd =>
            fields :+= field.toString
          case _ =>
        Some(fields.toList)
