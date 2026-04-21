package makarchess.api.service

import makarchess.api.dto.*
import makarchess.model.{ChessRules, ChessState, Color, Fen, PieceType}
import makarchess.parser.ParserModule
import makarchess.parser.api.ParserBackend
import makarchess.util.bot.BotHeuristics

final class ApiAnalysisService(registry: GameRegistry):
  import ApiError.*

  private val fenParser = ParserModule.fenParser(ParserBackend.Fast)

  def analyzeCurrent(): Either[ApiError, AnalysisResponse] =
    analyzeState(registry.currentController.model.chessState)

  def analyze(request: AnalysisRequest): Either[ApiError, AnalysisResponse] =
    stateFromRequest(request.fen).flatMap(analyzeState)

  private def analyzeState(state: ChessState): Either[ApiError, AnalysisResponse] =
    val legalMoves = ChessRules.legalMoves(state)
    val bestCapture =
      legalMoves
        .map(move => move -> BotHeuristics.captureValue(state, move))
        .filter(_._2 > 0)
        .sortBy { case (move, value) => (-value, ApiDtoMapper.moveToUci(move)) }
        .headOption
        .map { case (move, _) => ApiDtoMapper.moveToUci(move) }

    Right(
      AnalysisResponse(
        sideToMove = ApiDtoMapper.colorName(state.sideToMove),
        phase = ApiDtoMapper.toGamePhaseResponse(state.phase),
        isCheck = ApiDtoMapper.isCheck(state),
        legalMoves = legalMoves.size,
        material = materialResponse(state),
        bestCapture = bestCapture,
        legalMoveUcis = legalMoves.map(ApiDtoMapper.moveToUci).sorted
      )
    )

  private def stateFromRequest(fen: Option[String]): Either[ApiError, ChessState] =
    fen.map(_.trim).filter(_.nonEmpty) match
      case None => Right(registry.currentController.model.chessState)
      case Some(rawFen) =>
        for
          parsed <- fenParser.parse(rawFen).left.map(BadRequest.apply)
          state <- Fen.toChessState(parsed).left.map(BadRequest.apply)
        yield state

  private def materialResponse(state: ChessState): MaterialResponse =
    val white = materialFor(state, Color.White)
    val black = materialFor(state, Color.Black)
    MaterialResponse(white, black, white - black)

  private def materialFor(state: ChessState, color: Color): Int =
    state.board.values
      .filter(_.color == color)
      .map(piece => pieceValue(piece.kind))
      .sum

  private def pieceValue(pieceType: PieceType): Int =
    BotHeuristics.pieceValue.getOrElse(pieceType, 0)
