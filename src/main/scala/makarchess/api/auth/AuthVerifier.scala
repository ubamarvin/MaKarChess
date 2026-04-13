package makarchess.api.auth

final case class AuthenticatedUser(userId: String, email: Option[String] = None)

trait AuthVerifier:
  def verifyIdToken(idToken: String): Either[String, AuthenticatedUser]
