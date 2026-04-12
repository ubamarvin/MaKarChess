package makarchess.persistence

trait FileIO:
  def read(path: String): Either[String, String]
  def write(path: String, content: String): Either[String, Unit]
