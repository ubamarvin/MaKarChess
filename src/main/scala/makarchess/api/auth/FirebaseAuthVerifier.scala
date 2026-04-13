package makarchess.api.auth

import java.net.URL
import java.security.interfaces.RSAPublicKey

import com.auth0.jwk.UrlJwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT

final class FirebaseAuthVerifier(projectId: String) extends AuthVerifier:
  private val trimmedProjectId = projectId.trim
  private val issuer = s"https://securetoken.google.com/$trimmedProjectId"
  private val jwkProvider = new UrlJwkProvider(new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"))

  override def verifyIdToken(idToken: String): Either[String, AuthenticatedUser] =
    val trimmedToken = Option(idToken).map(_.trim).getOrElse("")
    if trimmedToken.isEmpty then Left("Missing bearer token.")
    else if trimmedProjectId.isEmpty then Left("Firebase project ID is not configured on the backend.")
    else
      try
        val decoded = JWT.decode(trimmedToken)
        verifyHeader(decoded)
        verifyClaims(decoded)
        val jwk = jwkProvider.get(decoded.getKeyId)
        val publicKey = jwk.getPublicKey match
          case rsaKey: RSAPublicKey => rsaKey
          case _ => throw new IllegalArgumentException("Firebase public key is not RSA.")
        val verifier = JWT
          .require(Algorithm.RSA256(publicKey, null))
          .withIssuer(issuer)
          .withAudience(trimmedProjectId)
          .build()
        val verified = verifier.verify(trimmedToken)
        val userId = Option(verified.getSubject).map(_.trim).filter(_.nonEmpty)
          .orElse(Option(verified.getClaim("user_id")).map(_.asString()).map(_.trim).filter(_.nonEmpty))
          .getOrElse(throw new IllegalArgumentException("Firebase token does not contain a user id."))
        Right(AuthenticatedUser(userId = userId, email = Option(verified.getClaim("email")).map(_.asString()).filter(_.nonEmpty)))
      catch
        case error: Exception => Left(s"Unauthorized: ${error.getMessage}")

  private def verifyHeader(decoded: DecodedJWT): Unit =
    val algorithm = Option(decoded.getAlgorithm).getOrElse("")
    if algorithm != "RS256" then
      throw new IllegalArgumentException("Firebase token must use RS256.")
    val keyId = Option(decoded.getKeyId).map(_.trim).getOrElse("")
    if keyId.isEmpty then
      throw new IllegalArgumentException("Firebase token is missing a key id.")

  private def verifyClaims(decoded: DecodedJWT): Unit =
    val subject = Option(decoded.getSubject).map(_.trim).getOrElse("")
    if subject.isEmpty then
      throw new IllegalArgumentException("Firebase token is missing a subject.")
    if subject.length > 128 then
      throw new IllegalArgumentException("Firebase token subject is too long.")
