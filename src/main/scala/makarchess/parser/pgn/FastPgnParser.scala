package makarchess.parser.pgn

import fastparse.*, NoWhitespace.*
import makarchess.model.PgnGame
import makarchess.parser.PgnParser
import makarchess.parser.shared.PgnSupport

final class FastPgnParser extends PgnParser:
  override def parse(input: String): Either[String, PgnGame] =
    fastparse.parse(input, pgnSections(using _)) match
      case Parsed.Success((tagLines, rawMoveText), _) =>
        PgnSupport.buildGame(tagLines, rawMoveText)
      case f: Parsed.Failure => Left(s"Invalid PGN: ${f.trace().longMsg}")

  override def render(game: PgnGame): String =
    PgnSupport.renderPgn(game)

  private def pgnSections[$: P]: P[(Seq[(String, String)], String)] =
    P(tagSection.? ~ whitespace ~ moveText.! ~ End)
      .map { case (tagLines, rawMoveText) => (tagLines.getOrElse(Seq.empty[(String, String)]), rawMoveText) }

  private def tagSection[$: P]: P[Seq[(String, String)]] = P((tagLine ~ whitespace).rep(1))

  private def tagLine[$: P]: P[(String, String)] =
    P("[" ~/ tagName.! ~ " " ~ quotedValue.! ~ "]").map { case (name, value) => (name, value.drop(1).dropRight(1)) }

  private def tagName[$: P]: P[Unit] = P(CharsWhile(c => c != ' ' && c != ']' && c != '\n' && c != '\r', min = 1))
  private def quotedValue[$: P]: P[Unit] = P("\"" ~~/ CharsWhile(_ != '"').rep ~ "\"")
  private def whitespace[$: P]: P[Unit] = P(CharsWhileIn(" \r\n\t").rep)
  private def moveText[$: P]: P[Unit] = P(AnyChar.rep)

object FastPgnParser:
  def apply(): FastPgnParser = new FastPgnParser()
