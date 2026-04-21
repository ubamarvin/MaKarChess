package makarchess.api.service

import makarchess.api.dto.*
import makarchess.model.{ChessRules, ChessState, Fen, Move}
import makarchess.parser.ParserModule
import makarchess.parser.api.ParserBackend
import makarchess.util.bot.BotFactory

final class ApiBotService(registry: GameRegistry):
  import ApiError.*

  private val fenParser = ParserModule.fenParser(ParserBackend.Fast)

  def availableBots(): BotTypesResponse =
    BotTypesResponse(BotFactory.availableNames)

  def chooseMove(request: BotMoveRequest): Either[ApiError, BotMoveResponse] =
    val botType = request.botType.map(_.trim).filter(_.nonEmpty).getOrElse("random")
    for
      state <- stateFromRequest(request.fen)
      bot <- BotFactory.fromName(botType).left.map(BadRequest.apply)
    yield
      val legalMoves = ChessRules.legalMoves(state)
      val chosen = bot.chooseMove(state).filter(move => legalMoves.exists(sameMove(_, move)))
      BotMoveResponse(
        botType = botType.toLowerCase,
        uci = chosen.map(ApiDtoMapper.moveToUci),
        from = chosen.map(move => ApiDtoMapper.toPositionResponse(move.from)),
        to = chosen.map(move => ApiDtoMapper.toPositionResponse(move.to)),
        promotion = chosen.flatMap(_.promotion).map(ApiDtoMapper.pieceTypeName),
        sideToMove = ApiDtoMapper.colorName(state.sideToMove),
        legalMoves = legalMoves.size,
        reason = if chosen.isEmpty then Some("No legal move available.") else None
      )

  private def stateFromRequest(fen: Option[String]): Either[ApiError, ChessState] =
    fen.map(_.trim).filter(_.nonEmpty) match
      case None => Right(registry.currentController.model.chessState)
      case Some(rawFen) =>
        for
          parsed <- fenParser.parse(rawFen).left.map(BadRequest.apply)
          state <- Fen.toChessState(parsed).left.map(BadRequest.apply)
        yield state

  private def sameMove(a: Move, b: Move): Boolean =
    a.from == b.from && a.to == b.to && a.promotion == b.promotion
