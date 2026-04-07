package makarchess.util

import makarchess.model.MoveAttemptError

sealed trait MoveResult[+A]:

  def map[B](f: A => B): MoveResult[B] =
    this match
      case MoveResult.Ok(value) => MoveResult.Ok(f(value))
      case e: MoveResult.Err    => e

  def flatMap[B](f: A => MoveResult[B]): MoveResult[B] =
    this match
      case MoveResult.Ok(value) => f(value)
      case e: MoveResult.Err    => e

object MoveResult:

  final case class Ok[+A](value: A) extends MoveResult[A]

  final case class Err(error: MoveAttemptError) extends MoveResult[Nothing]

  def pure[A](value: A): MoveResult[A] = Ok(value)

  def fail(error: MoveAttemptError): MoveResult[Nothing] = Err(error)
