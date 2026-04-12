package makarchess.parser.fastparse

import makarchess.model.Fen
import makarchess.parser.FenParser
import makarchess.parser.fen.FastFenParser

final class FastParseFenParser extends FenParser:
  private val delegate = FastFenParser()

  override def parse(input: String): Either[String, Fen] =
    delegate.parse(input)

  override def render(fen: Fen): String =
    delegate.render(fen)

object FastParseFenParser:
  def apply(): FastParseFenParser = new FastParseFenParser()
