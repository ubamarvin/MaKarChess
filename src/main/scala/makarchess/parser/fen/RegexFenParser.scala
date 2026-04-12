package makarchess.parser.fen

import makarchess.model.Fen
import makarchess.parser.FenParser
import makarchess.parser.shared.FenSupport

final class RegexFenParser extends FenParser:
  private val fenRegex = raw"^([^\s]+)\s+([wb])\s+([^\s]+)\s+([^\s]+)\s+(\d+)\s+(\d+)$$".r

  override def parse(input: String): Either[String, Fen] =
    input.trim match
      case fenRegex(placement, side, castling, enPassant, halfmove, fullmove) =>
        FenSupport.buildFen(placement, side, castling, enPassant, halfmove, fullmove)
      case _ => Left("Invalid FEN: expected '<board> <side> <castling> <enPassant> <halfmove> <fullmove>'")

  override def render(fen: Fen): String =
    FenSupport.renderFen(fen)

object RegexFenParser:
  def apply(): RegexFenParser = new RegexFenParser()
