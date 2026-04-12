package makarchess.parser.fen

import fastparse.*, NoWhitespace.*
import makarchess.model.Fen
import makarchess.parser.FenParser
import makarchess.parser.shared.FenSupport

final class FastFenParser extends FenParser:
  override def parse(input: String): Either[String, Fen] =
    fastparse.parse(input.trim, fenFields(using _)) match
      case Parsed.Success((placement, side, castling, enPassant, halfmove, fullmove), _) =>
        FenSupport.buildFen(placement, side, castling, enPassant, halfmove, fullmove)
      case f: Parsed.Failure => Left(s"Invalid FEN: ${f.trace().longMsg}")

  override def render(fen: Fen): String =
    FenSupport.renderFen(fen)

  private def fenFields[$: P]: P[(String, String, String, String, String, String)] =
    P(piecePlacement.! ~ " " ~ sideToMove.! ~ " " ~ castling.! ~ " " ~ enPassant.! ~ " " ~ number.! ~ " " ~ number.! ~ End)

  private def piecePlacement[$: P]: P[Unit] = P(CharsWhile(_ != ' ', min = 1))
  private def sideToMove[$: P]: P[Unit] = P(CharIn("wb"))
  private def castling[$: P]: P[Unit] = P(CharsWhile(c => c != ' ' && c != '\n' && c != '\r', min = 1))
  private def enPassant[$: P]: P[Unit] = P(CharsWhile(c => c != ' ' && c != '\n' && c != '\r', min = 1))
  private def number[$: P]: P[Unit] = P(CharIn("0-9").rep(1))

object FastFenParser:
  def apply(): FastFenParser = new FastFenParser()
