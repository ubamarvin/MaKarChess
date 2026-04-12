package makarchess.parser.fen

import makarchess.model.Fen
import makarchess.parser.FenParser
import makarchess.parser.shared.FenSupport

import scala.util.parsing.combinator.RegexParsers

final class CombinatorFenParser extends FenParser, RegexParsers:
  override val skipWhitespace: Boolean = false

  override def parse(input: String): Either[String, Fen] =
    parseAll(fenFields, input.trim) match
      case Success((placement, side, castling, enPassant, halfmove, fullmove), _) =>
        FenSupport.buildFen(placement, side, castling, enPassant, halfmove, fullmove)
      case Failure(msg, next) =>
        Left(s"Invalid FEN: $msg at line ${next.pos.line}, column ${next.pos.column}")
      case Error(msg, next) =>
        Left(s"Invalid FEN: $msg at line ${next.pos.line}, column ${next.pos.column}")

  override def render(fen: Fen): String =
    FenSupport.renderFen(fen)

  private def fenFields: Parser[(String, String, String, String, String, String)] =
    piecePlacement ~ " " ~ sideToMove ~ " " ~ castling ~ " " ~ enPassant ~ " " ~ number ~ " " ~ number ^^ {
      case placement ~ _ ~ side ~ _ ~ castlingField ~ _ ~ enPassantField ~ _ ~ halfmove ~ _ ~ fullmove =>
        (placement, side, castlingField, enPassantField, halfmove, fullmove)
    }

  private def piecePlacement: Parser[String] = """[^\s]+""".r
  private def sideToMove: Parser[String] = "w|b".r
  private def castling: Parser[String] = """[^\s]+""".r
  private def enPassant: Parser[String] = """[^\s]+""".r
  private def number: Parser[String] = """\d+""".r

object CombinatorFenParser:
  def apply(): CombinatorFenParser = new CombinatorFenParser()
