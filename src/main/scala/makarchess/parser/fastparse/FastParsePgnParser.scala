package makarchess.parser.fastparse

import makarchess.model.PgnGame
import makarchess.parser.PgnParser
import makarchess.parser.pgn.FastPgnParser

final class FastParsePgnParser extends PgnParser:
  private val delegate = FastPgnParser()

  override def parse(input: String): Either[String, PgnGame] =
    delegate.parse(input)

  override def render(game: PgnGame): String =
    delegate.render(game)

object FastParsePgnParser:
  def apply(): FastParsePgnParser = new FastParsePgnParser()
