package makarchess.parser.api

import makarchess.model.Fen

trait FenParser extends ChessParser[Fen]:
  def render(fen: Fen): String
