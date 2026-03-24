enum Color:
  case White, Black

enum PieceType:
  case King, Queen, Rook, Bishop, Knight, Pawn

case class Piece(color: Color, kind: PieceType)

case class Position(file: Char, rank: Int)

object Position:
  def from(file: Char, rank: Int): Either[GameError, Position] =
    createPosition(file, rank)

case class Move(from: Position, to: Position)

object Move:
  def fromString(input: String): Either[GameError, Move] =
    parseMove(input)

case class Board(pieces: Map[Position, Piece])

case class GameState(board: Board, currentPlayer: Color)

enum GameError:
  case InvalidMoveFormat
  case InvalidPosition
  case NoPieceAtSource
  case WrongPlayer
  case SameColorCapture

def createPosition(file: Char, rank: Int): Either[GameError, Position] =
  val normalizedFile = file.toLower
  if normalizedFile >= 'a' && normalizedFile <= 'h' && rank >= 1 && rank <= 8 then
    Right(Position(normalizedFile, rank))
  else
    Left(GameError.InvalidPosition)

def createMove(from: Position, to: Position): Move =
  Move(from, to)

def parseMove(input: String): Either[GameError, Move] =
  val compact = input.trim
  if compact.length != 4 then
    Left(GameError.InvalidMoveFormat)
  else
    for
      from <- createPosition(compact(0), compact(1).asDigit)
      to <- createPosition(compact(2), compact(3).asDigit)
    yield createMove(from, to)

def initialBoard: Board =
  val whiteBackRank = List(
    'a' -> Piece(Color.White, PieceType.Rook),
    'b' -> Piece(Color.White, PieceType.Knight),
    'c' -> Piece(Color.White, PieceType.Bishop),
    'd' -> Piece(Color.White, PieceType.Queen),
    'e' -> Piece(Color.White, PieceType.King),
    'f' -> Piece(Color.White, PieceType.Bishop),
    'g' -> Piece(Color.White, PieceType.Knight),
    'h' -> Piece(Color.White, PieceType.Rook)
  )
  val blackBackRank = whiteBackRank.map { case (file, piece) =>
    file -> piece.copy(color = Color.Black)
  }
  val whitePawns = ('a' to 'h').map(file => Position(file, 2) -> Piece(Color.White, PieceType.Pawn))
  val blackPawns = ('a' to 'h').map(file => Position(file, 7) -> Piece(Color.Black, PieceType.Pawn))
  val whiteBack = whiteBackRank.map { case (file, piece) => Position(file, 1) -> piece }
  val blackBack = blackBackRank.map { case (file, piece) => Position(file, 8) -> piece }

  Board((whiteBack ++ blackBack ++ whitePawns ++ blackPawns).toMap)

def pieceAt(board: Board, position: Position): Option[Piece] =
  board.pieces.get(position)

def placePiece(board: Board, position: Position, piece: Piece): Board =
  board.copy(pieces = board.pieces.updated(position, piece))

def removePiece(board: Board, position: Position): Board =
  board.copy(pieces = board.pieces - position)

def relocatePiece(board: Board, move: Move): Board =
  pieceAt(board, move.from) match
    case Some(piece) =>
      val withoutSource = removePiece(board, move.from)
      placePiece(withoutSource, move.to, piece)
    case None => board

def opposite(color: Color): Color =
  color match
    case Color.White => Color.Black
    case Color.Black => Color.White

def initialGameState: GameState =
  GameState(initialBoard, Color.White)

def applyMove(state: GameState, move: Move): Either[GameError, GameState] =
  pieceAt(state.board, move.from) match
    case None => Left(GameError.NoPieceAtSource)
    case Some(piece) if piece.color != state.currentPlayer =>
      Left(GameError.WrongPlayer)
    case Some(piece) =>
      pieceAt(state.board, move.to) match
        case Some(targetPiece) if targetPiece.color == piece.color =>
          Left(GameError.SameColorCapture)
        case _ =>
          Right(
            state.copy(
              board = relocatePiece(state.board, move),
              currentPlayer = opposite(state.currentPlayer)
            )
          )

def renderPiece(piece: Piece): Char =
  val symbol = piece.kind match
    case PieceType.King   => 'k'
    case PieceType.Queen  => 'q'
    case PieceType.Rook   => 'r'
    case PieceType.Bishop => 'b'
    case PieceType.Knight => 'n'
    case PieceType.Pawn   => 'p'

  piece.color match
    case Color.White => symbol.toUpper
    case Color.Black => symbol

def renderBoard(board: Board): String =
  val files = ('a' to 'h').mkString(" ")
  val rows = (8 to 1 by -1).map { rank =>
    val squares = ('a' to 'h').map { file =>
      val position = Position(file, rank)
      pieceAt(board, position).map(renderPiece).getOrElse('.')
    }.mkString(" ")
    s"$rank $squares $rank"
  }

  (Seq(s"  $files") ++ rows ++ Seq(s"  $files")).mkString("\n")

def renderCurrentPlayer(color: Color): String =
  color match
    case Color.White => "White to move"
    case Color.Black => "Black to move"

private def renderGameError(error: GameError): String =
  error match
    case GameError.InvalidMoveFormat => "Invalid move format. Expected format like e2e4."
    case GameError.InvalidPosition   => "Invalid position in move."
    case GameError.NoPieceAtSource   => "No piece at source square."
    case GameError.WrongPlayer       => "That piece belongs to the other player."
    case GameError.SameColorCapture  => "Cannot capture your own piece."

trait ConsoleIO:
  def readLine(): Option[String]
  def printLine(line: String): Unit

class StdConsoleIO extends ConsoleIO:
  override def readLine(): Option[String] =
    Option(scala.io.StdIn.readLine())

  override def printLine(line: String): Unit =
    println(line)

def runGame(io: ConsoleIO): Unit =
  @annotation.tailrec
  def loop(state: GameState): Unit =
    io.printLine(renderBoard(state.board))
    io.printLine(renderCurrentPlayer(state.currentPlayer))
    io.printLine("Enter move (e.g. e2e4) or 'quit':")

    io.readLine() match
      case None =>
        io.printLine("Goodbye.")
      case Some(rawInput) =>
        val input = rawInput.trim.toLowerCase
        if input == "quit" || input == "exit" then
          io.printLine("Goodbye.")
        else
          parseMove(input) match
            case Left(error) =>
              io.printLine(s"Error: ${renderGameError(error)}")
              loop(state)
            case Right(move) =>
              applyMove(state, move) match
                case Left(error) =>
                  io.printLine(s"Error: ${renderGameError(error)}")
                  loop(state)
                case Right(nextState) =>
                  loop(nextState)

  loop(initialGameState)

object Main:
  def main(args: Array[String]): Unit =
    runGame(StdConsoleIO())