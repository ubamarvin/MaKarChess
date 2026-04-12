package makarchess.parser.shared

import makarchess.model.*

object FenSupport:
  def buildFen(
      placement: String,
      side: String,
      castlingField: String,
      enPassantField: String,
      halfmove: String,
      fullmove: String
  ): Either[String, Fen] =
    for
      board <- parseBoard(placement)
      sideToMove <- parseSide(side)
      castlingRights <- parseCastling(castlingField)
      enPassantSquare <- parseEnPassant(enPassantField)
      halfmoveClock <- parseInt(halfmove, "halfmove clock")
      fullmoveNumber <- parseInt(fullmove, "fullmove number")
    yield Fen(board, sideToMove, castlingRights, enPassantSquare, halfmoveClock, fullmoveNumber)

  def renderFen(fen: Fen): String =
    val placement =
      (8 to 1 by -1).map { rank =>
        var empty = 0
        val builder = new StringBuilder
        for file <- 'a' to 'h' do
          fen.board.get(Position(file, rank)) match
            case Some(piece) =>
              if empty > 0 then
                builder.append(empty)
                empty = 0
              builder.append(renderPiece(piece))
            case None =>
              empty += 1
        if empty > 0 then builder.append(empty)
        builder.toString
      }.mkString("/")

    val side = fen.sideToMove match
      case Color.White => "w"
      case Color.Black => "b"

    val castling = renderCastling(fen.castling)
    val enPassant = fen.enPassant.map(pos => s"${pos.file}${pos.rank}").getOrElse("-")
    s"$placement $side $castling $enPassant ${fen.halfmoveClock} ${fen.fullmoveNumber}"

  private def renderCastling(castling: CastlingRights): String =
    val value =
      Seq(
        if castling.whiteKingside then Some("K") else None,
        if castling.whiteQueenside then Some("Q") else None,
        if castling.blackKingside then Some("k") else None,
        if castling.blackQueenside then Some("q") else None
      ).flatten.mkString
    if value.isEmpty then "-" else value

  private def renderPiece(piece: Piece): Char =
    (piece.color, piece.kind) match
      case (Color.White, PieceType.King)   => 'K'
      case (Color.White, PieceType.Queen)  => 'Q'
      case (Color.White, PieceType.Rook)   => 'R'
      case (Color.White, PieceType.Bishop) => 'B'
      case (Color.White, PieceType.Knight) => 'N'
      case (Color.White, PieceType.Pawn)   => 'P'
      case (Color.Black, PieceType.King)   => 'k'
      case (Color.Black, PieceType.Queen)  => 'q'
      case (Color.Black, PieceType.Rook)   => 'r'
      case (Color.Black, PieceType.Bishop) => 'b'
      case (Color.Black, PieceType.Knight) => 'n'
      case (Color.Black, PieceType.Pawn)   => 'p'

  private def parseBoard(raw: String): Either[String, Map[Position, Piece]] =
    val ranks = raw.split('/')
    if ranks.length != 8 then Left("piece placement must contain 8 ranks")
    else
      ranks.zipWithIndex.foldLeft[Either[String, Map[Position, Piece]]](Right(Map.empty)) { case (acc, (rankText, index)) =>
        for
          board <- acc
          rankBoard <- parseRank(rankText, 8 - index)
        yield board ++ rankBoard
      }

  private def parseRank(rankText: String, rank: Int): Either[String, Map[Position, Piece]] =
    val init: Either[String, (Int, Map[Position, Piece])] = Right(0 -> Map.empty)
    rankText.foldLeft(init) { (acc, ch) =>
      acc.flatMap { case (fileIndex, pieces) =>
        if ch.isDigit then
          val empty = ch.asDigit
          if empty < 1 || empty > 8 then Left("rank contains invalid digit")
          else Right((fileIndex + empty) -> pieces)
        else
          decodePiece(ch).flatMap { piece =>
            if fileIndex >= 8 then Left("rank exceeds 8 files")
            else
              val pos = Position(('a' + fileIndex).toChar, rank)
              Right((fileIndex + 1) -> (pieces + (pos -> piece)))
          }
      }
    }.flatMap { case (fileIndex, pieces) =>
      if fileIndex == 8 then Right(pieces)
      else Left("each rank must cover exactly 8 files")
    }

  private def decodePiece(ch: Char): Either[String, Piece] =
    ch match
      case 'K' => Right(Piece(Color.White, PieceType.King))
      case 'Q' => Right(Piece(Color.White, PieceType.Queen))
      case 'R' => Right(Piece(Color.White, PieceType.Rook))
      case 'B' => Right(Piece(Color.White, PieceType.Bishop))
      case 'N' => Right(Piece(Color.White, PieceType.Knight))
      case 'P' => Right(Piece(Color.White, PieceType.Pawn))
      case 'k' => Right(Piece(Color.Black, PieceType.King))
      case 'q' => Right(Piece(Color.Black, PieceType.Queen))
      case 'r' => Right(Piece(Color.Black, PieceType.Rook))
      case 'b' => Right(Piece(Color.Black, PieceType.Bishop))
      case 'n' => Right(Piece(Color.Black, PieceType.Knight))
      case 'p' => Right(Piece(Color.Black, PieceType.Pawn))
      case _   => Left(s"invalid piece character '$ch'")

  private def parseSide(raw: String): Either[String, Color] =
    raw match
      case "w" => Right(Color.White)
      case "b" => Right(Color.Black)
      case _   => Left("active color must be 'w' or 'b'")

  private def parseCastling(raw: String): Either[String, CastlingRights] =
    if raw == "-" then Right(CastlingRights(false, false, false, false))
    else
      val allowed = Set('K', 'Q', 'k', 'q')
      if raw.isEmpty || raw.exists(ch => !allowed.contains(ch)) || raw.distinct.length != raw.length then Left("invalid castling field")
      else
        Right(
          CastlingRights(
            whiteKingside = raw.contains('K'),
            whiteQueenside = raw.contains('Q'),
            blackKingside = raw.contains('k'),
            blackQueenside = raw.contains('q')
          )
        )

  private def parseEnPassant(raw: String): Either[String, Option[Position]] =
    if raw == "-" then Right(None)
    else if raw.length != 2 then Left("invalid en-passant square")
    else Position.from(raw(0), raw(1).asDigit).map(Some(_))

  private def parseInt(raw: String, fieldName: String): Either[String, Int] =
    raw.toIntOption match
      case Some(value) if value >= 0 => Right(value)
      case _                         => Left(s"invalid $fieldName")
