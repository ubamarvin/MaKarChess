package makarchess.model

case class PgnReplayCursor(states: Vector[ChessState], index: Int):
  def currentState: Either[String, ChessState] =
    states.lift(index).toRight(s"Replay index out of bounds: $index")

  def stepForward(): Either[String, PgnReplayCursor] =
    if index + 1 < states.length then Right(copy(index = index + 1))
    else Left("Already at end of replay.")

  def stepBackward(): Either[String, PgnReplayCursor] =
    if index > 0 then Right(copy(index = index - 1))
    else Left("Already at start of replay.")

  def jumpToStart(): PgnReplayCursor =
    copy(index = 0)

  def jumpToEnd(): PgnReplayCursor =
    copy(index = math.max(0, states.length - 1))

  def jumpTo(targetIndex: Int): Either[String, PgnReplayCursor] =
    if targetIndex >= 0 && targetIndex < states.length then Right(copy(index = targetIndex))
    else Left(s"Replay index out of bounds: $targetIndex")
