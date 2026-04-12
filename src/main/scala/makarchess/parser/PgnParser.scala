package makarchess.parser

import makarchess.model.PgnGame

trait PgnParser extends Parser[PgnGame], makarchess.parser.api.PgnParser
