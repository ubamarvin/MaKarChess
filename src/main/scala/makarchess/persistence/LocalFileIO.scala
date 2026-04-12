package makarchess.persistence

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

final class LocalFileIO extends FileIO:
  override def read(path: String): Either[String, String] =
    try Right(Files.readString(Paths.get(path), StandardCharsets.UTF_8))
    catch case e: Exception => Left(s"Could not read file '$path': ${e.getMessage}")

  override def write(path: String, content: String): Either[String, Unit] =
    try
      Files.writeString(Paths.get(path), content, StandardCharsets.UTF_8)
      Right(())
    catch case e: Exception => Left(s"Could not write file '$path': ${e.getMessage}")

object LocalFileIO:
  def apply(): LocalFileIO = new LocalFileIO()
