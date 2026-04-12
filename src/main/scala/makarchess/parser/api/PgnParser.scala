package makarchess.parser.api

import makarchess.model.PgnGame

trait PgnParser extends ChessParser[PgnGame]:
  def render(game: PgnGame): String
