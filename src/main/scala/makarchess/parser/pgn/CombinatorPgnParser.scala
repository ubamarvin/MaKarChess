package makarchess.parser.pgn

import makarchess.model.PgnGame
import makarchess.parser.PgnParser
import makarchess.parser.shared.PgnSupport

import scala.util.parsing.combinator.RegexParsers

final class CombinatorPgnParser extends PgnParser, RegexParsers:
  override val skipWhitespace: Boolean = false

  override def parse(input: String): Either[String, PgnGame] =
    parseAll(pgnSections, input) match
      case Success((tags, moves), _) =>
        PgnSupport.buildGame(tags, moves)
      case Failure(msg, next) => Left(s"Invalid PGN: $msg at line ${next.pos.line}, column ${next.pos.column}")
      case Error(msg, next)   => Left(s"Invalid PGN: $msg at line ${next.pos.line}, column ${next.pos.column}")

  override def render(game: PgnGame): String =
    PgnSupport.renderPgn(game)

  private def pgnSections: Parser[(Seq[(String, String)], String)] =
    tagSection.? ~ whitespace ~ moveText ^^ {
      case tags ~ _ ~ moves => (tags.getOrElse(Seq.empty), moves)
    }

  private def tagSection: Parser[Seq[(String, String)]] = rep1(tagLine <~ whitespace)

  private def tagLine: Parser[(String, String)] =
    "[" ~> tagName ~ (" " ~> quotedValue) <~ "]" ^^ { case name ~ value => (name, value) }

  private def tagName: Parser[String] = """[^\s\]]+""".r
  private def quotedValue: Parser[String] = "\"" ~> """[^"]*""".r <~ "\""
  private def whitespace: Parser[String] = """[ \r\n\t]*""".r
  private def moveText: Parser[String] = """(?s).*""".r

object CombinatorPgnParser:
  def apply(): CombinatorPgnParser = new CombinatorPgnParser()
