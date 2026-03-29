package makarchess.model

import makarchess.util.Observable

case class GameSnapshot(
    boardText: String,
    /** Derived only from model state (check, mate, stalemate, draws). */
    statusLine: String,
    currentPlayerLine: String,
    phase: GamePhase
)

object ChessModel:

  def formatError(error: MoveAttemptError): String =
    error match
      case MoveAttemptError.InvalidInput    => "Invalid move format. Use e2e4 or e7e8q."
      case MoveAttemptError.IllegalMove     => "Illegal move."
      case MoveAttemptError.GameAlreadyOver => "The game is already over."

class ChessModel private (private var state: ChessState) extends Observable:

  def this() = this(ChessRules.initialState)

  def snapshot: GameSnapshot =
    buildSnapshot(state)

  /** Reset to the standard starting position and notify observers. */
  def restart(): Unit =
    state = ChessRules.initialState
    notifyObservers

  private def buildSnapshot(state: ChessState): GameSnapshot =
    val boardText = ChessRules.renderBoard(state.board)
    val (statusLine, playerLine) = state.phase match
      case GamePhase.InProgress =>
        val check =
          ChessRules.isInCheck(state.board, state.sideToMove, state.enPassant)
        val st = if check then "Check." else ""
        (st, playerToMoveLine(state.sideToMove))
      case GamePhase.Checkmate(winner) =>
        (s"Checkmate. ${winner} wins.", "")
      case GamePhase.Stalemate =>
        ("Stalemate. Draw.", "")
      case GamePhase.DrawFiftyMoveRule =>
        ("Draw by fifty-move rule.", "")
      case GamePhase.DrawThreefoldRepetition =>
        ("Draw by threefold repetition.", "")
      case GamePhase.DrawInsufficientMaterial =>
        ("Draw by insufficient material.", "")

    GameSnapshot(
      boardText = boardText,
      statusLine = statusLine,
      currentPlayerLine = playerLine,
      phase = state.phase
    )

  private def playerToMoveLine(side: Color): String =
    side match
      case Color.White => "White to move"
      case Color.Black => "Black to move"

  /** Parses and applies a move; notifies observers only after a successful, legal move. */
  def tryMove(input: String): Either[MoveAttemptError, Unit] =
    if state.phase != GamePhase.InProgress then Left(MoveAttemptError.GameAlreadyOver)
    else
      ChessRules.parseUci(input) match
        case Left(e) => Left(e)
        case Right(move) =>
          val legal = ChessRules.legalMoves(state)
          if !legal.exists(sameMove(_, move)) then Left(MoveAttemptError.IllegalMove)
          else
            state = ChessRules.applyLegalMove(state, move)
            notifyObservers
            Right(())

  private def sameMove(a: Move, b: Move): Boolean =
    a.from == b.from && a.to == b.to && a.promotion == b.promotion
