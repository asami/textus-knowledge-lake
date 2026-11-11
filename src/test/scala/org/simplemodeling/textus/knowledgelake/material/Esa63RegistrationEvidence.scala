package org.simplemodeling.textus.knowledgelake.material

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path}
import io.circe.{Json, parser}

/*
 * Private observations of actual independent SQL reads, without client-success authority.
 *
 * @since   Oct. 25, 2026
 * @version Nov.  6, 2026
 */
private[material] object Esa63RegistrationEvidence {
  def awaitRegistered(work: Path, readRows: () => Vector[Map[String, Option[String]]],
    deadlineNanos: Long): Vector[Map[String, Option[String]]] = {
    var rows = readRows()
    while (!rows.exists(_.get("state").flatten.contains("registered"))) {
      if (Files.exists(work.resolve("stop.request")))
        throw new IllegalStateException("Native registration observation stopped before registration")
      if (System.nanoTime() - deadlineNanos >= 0)
        throw new IllegalStateException("Native registration observation deadline elapsed")
      Thread.sleep(100)
      rows = readRows()
    }
    val observation = Json.obj(
      "source" -> Json.fromString("independent-postgresql"),
      "registrationRows" -> Json.fromInt(rows.size),
      "registeredRows" -> Json.fromInt(rows.count(_.get("state").flatten.contains("registered"))),
      "clientCompletionObserved" -> Json.fromBoolean(Files.exists(work.resolve("android.done"))),
      "rows" -> Json.fromValues(rows.map(_row)))
    val bytes = observation.spaces2.getBytes(UTF_8)
    if (bytes.length > 8388608)
      throw new IllegalStateException("Native registration observation exceeds the evidence size bound")
    Files.write(work.resolve("native-registration-observation.json"), bytes)
    rows
  }

  private def _row(row: Map[String, Option[String]]): Json = Json.fromFields(
    Vector("id", "materialRef", "state", "owner_id").map(key =>
      key -> row.get(key).flatten.fold(Json.Null)(Json.fromString)) :+
      ("document" -> row.get("document").flatten.fold(Json.Null)(value =>
        parser.parse(value).getOrElse(Json.fromString(value)))))
}
