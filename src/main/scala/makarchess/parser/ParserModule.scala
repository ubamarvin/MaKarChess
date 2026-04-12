package makarchess.parser

import makarchess.parser.api.ParserBackend
import makarchess.parser.fen.{CombinatorFenParser, FastFenParser, RegexFenParser}
import makarchess.parser.pgn.{CombinatorPgnParser, FastPgnParser, RegexPgnParser}

object ParserModule:
  def fenParser(backend: ParserBackend): FenParser =
    backend match
      case ParserBackend.Fast       => FastFenParser()
      case ParserBackend.Combinator => CombinatorFenParser()
      case ParserBackend.Regex      => RegexFenParser()

  def pgnParser(backend: ParserBackend): PgnParser =
    backend match
      case ParserBackend.Fast       => FastPgnParser()
      case ParserBackend.Combinator => CombinatorPgnParser()
      case ParserBackend.Regex      => RegexPgnParser()
