package makarchess.parser.pgn

import makarchess.model.PgnGame
import makarchess.parser.PgnParser
import makarchess.parser.shared.PgnSupport

final class RegexPgnParser extends PgnParser:
  private val tagRegex = raw"\[([^\s\]]+)\s+\"([^\"]*)\"\]".r

  override def parse(input: String): Either[String, PgnGame] =
    val trimmed = input.trim
    val lines = trimmed.linesIterator.toList
    val (tagLines, moveLines) = lines.span(_.trim.startsWith("["))

    val parsedTags =
      tagLines.foldLeft[Either[String, Vector[(String, String)]]](Right(Vector.empty)) { (acc, line) =>
        acc.flatMap { tags =>
          line.trim match
            case tagRegex(name, value) => Right(tags :+ (name -> value))
            case bad if bad.nonEmpty    => Left(s"Invalid PGN tag: $bad")
            case _                      => Right(tags)
        }
      }

    parsedTags.flatMap(tags => PgnSupport.buildGame(tags, moveLines.mkString("\n")).left.map(err => s"Invalid PGN: $err"))

  override def render(game: PgnGame): String =
    PgnSupport.renderPgn(game)

object RegexPgnParser:
  def apply(): RegexPgnParser = new RegexPgnParser()
