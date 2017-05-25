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
*
* From https://raw.githubusercontent.com/tototoshi/scala-csv/master/src/main/scala/com/github/tototoshi/csv/CSVParser.scala
* Removed escape char
*/
import scala.annotation.switch

class MalformedCSVException(message: String) extends Exception(message)

object CSVParser {

  private type State = Int
  private final val Start = 0
  private final val Field = 1
  private final val Delimiter = 2
  private final val End = 3
  private final val QuoteStart = 4
  private final val QuoteEnd = 5
  private final val QuotedField = 6

  /**
   * {{{
   * scala> com.github.tototoshi.csv.CSVParser.parse("a,b,c", '\\', ',', '"')
   * res0: Option[List[String]] = Some(List(a, b, c))
   *
   * scala> com.github.tototoshi.csv.CSVParser.parse("\"a\",\"b\",\"c\"", '\\', ',', '"')
   * res1: Option[List[String]] = Some(List(a, b, c))
   * }}}
   */
  def parse(input: String, delimiter: Char, quoteChar: Char): Option[List[String]] = {
    val buf: Array[Char] = input.toCharArray
    var fields: Vector[String] = Vector()
    var field = new StringBuilder
    var state: State = Start
    var pos = 0
    val buflen = buf.length

    if (buf.length > 0 && buf(0) == '\uFEFF') {
      pos += 1
    }

    while (state != End && pos < buflen) {
      val c = buf(pos)
      (state: @switch) match {
        case Start => {
          c match {
            case `quoteChar` => {
              state = QuoteStart
              pos += 1
            }
            case `delimiter` => {
              fields :+= field.toString
              field = new StringBuilder
              state = Delimiter
              pos += 1
            }
            case '\n' | '\u2028' | '\u2029' | '\u0085' => {
              fields :+= field.toString
              field = new StringBuilder
              state = End
              pos += 1
            }
            case '\r' => {
              if (pos + 1 < buflen && buf(1) == '\n') {
                pos += 1
              }
              fields :+= field.toString
              field = new StringBuilder
              state = End
              pos += 1
            }
            case x => {
              field += x
              state = Field
              pos += 1
            }
          }
        }
        case Delimiter => {
          c match {
            case `quoteChar` => {
              state = QuoteStart
              pos += 1
            }
            case `delimiter` => {
              fields :+= field.toString
              field = new StringBuilder
              state = Delimiter
              pos += 1
            }
            case '\n' | '\u2028' | '\u2029' | '\u0085' => {
              fields :+= field.toString
              field = new StringBuilder
              state = End
              pos += 1
            }
            case '\r' => {
              if (pos + 1 < buflen && buf(1) == '\n') {
                pos += 1
              }
              fields :+= field.toString
              field = new StringBuilder
              state = End
              pos += 1
            }
            case x => {
              field += x
              state = Field
              pos += 1
            }
          }
        }
        case Field => {
          c match {
            case `delimiter` => {
              fields :+= field.toString
              field = new StringBuilder
              state = Delimiter
              pos += 1
            }
            case '\n' | '\u2028' | '\u2029' | '\u0085' => {
              fields :+= field.toString
              field = new StringBuilder
              state = End
              pos += 1
            }
            case '\r' => {
              if (pos + 1 < buflen && buf(1) == '\n') {
                pos += 1
              }
              fields :+= field.toString
              field = new StringBuilder
              state = End
              pos += 1
            }
            case x => {
              field += x
              state = Field
              pos += 1
            }
          }
        }
        case QuoteStart => {
          c match {
            case `quoteChar` => {
              if (pos + 1 < buflen && buf(pos + 1) == quoteChar) {
                field += quoteChar
                state = QuotedField
                pos += 2
              } else {
                state = QuoteEnd
                pos += 1
              }
            }
            case x => {
              field += x
              state = QuotedField
              pos += 1
            }
          }
        }
        case QuoteEnd => {
          c match {
            case `delimiter` => {
              fields :+= field.toString
              field = new StringBuilder
              state = Delimiter
              pos += 1
            }
            case '\n' | '\u2028' | '\u2029' | '\u0085' => {
              fields :+= field.toString
              field = new StringBuilder
              state = End
              pos += 1
            }
            case '\r' => {
              if (pos + 1 < buflen && buf(1) == '\n') {
                pos += 1
              }
              fields :+= field.toString
              field = new StringBuilder
              state = End
              pos += 1
            }
            case _ => {
              throw new MalformedCSVException(buf.mkString)
            }
          }
        }
        case QuotedField => {
          c match {
            case `quoteChar` => {
              if (pos + 1 < buflen && buf(pos + 1) == quoteChar) {
                field += quoteChar
                state = QuotedField
                pos += 2
              } else {
                state = QuoteEnd
                pos += 1
              }
            }
            case x => {
              field += x
              state = QuotedField
              pos += 1
            }
          }
        }
        case End => {
          sys.error("unexpected error")
        }
      }
    }
    (state: @switch) match {
      case Delimiter => {
        fields :+= ""
        Some(fields.toList)
      }
      case QuotedField => {
        None
      }
      case _ => {
        if (!field.isEmpty) {
          // When no crlf at end of file
          state match {
            case Field | QuoteEnd => {
              fields :+= field.toString
            }
            case _ => {
            }
          }
        }
        Some(fields.toList)
      }
    }
  }
}
