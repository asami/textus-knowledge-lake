package org.simplemodeling.textus.knowledgelake.material

import scala.util.Using
import io.circe.{Json, parser}
import org.goldenport.cncf.datastore.DataStore
import org.goldenport.cncf.http.HttpDriver
import org.goldenport.datatype.{ContentType, MimeBody}
import org.goldenport.bag.Bag
import org.goldenport.http.HttpResponse
import org.goldenport.protocol.Property
import org.goldenport.record.Record

/* Native delivery operation fixtures and independent committed checkpoints.
 * @since Oct. 25, 2026
 * @version Nov.  6, 2026
 */
private[material] object MaterialDeliveryOperationsFixture {
  import MaterialRegistrationPersistenceFixture.*
  import MaterialRegistrationOperationsFixture.{connectionId, storeName, destinationParent, pngs}
  val deliveryTable: String = DataStore.CollectionId.EntityStore(MaterialDeliveryEntity.collectionId).collectionName
  val requestId: String = "mobile-book-1"
  def configuration(id: String = binding, parent: String = destinationParent): Json = MaterialDeliveryConfiguration(
    id, "Mobile Materials " + id, Vector(principal, foreign), connectionId, storeName, parent, lake, logicalParent, 1000000,
    preferDirect = false).json
  def parameters(config: Option[Json] = Some(configuration())): Map[String, String] =
    MaterialRegistrationOperationsFixture.parameters(None) ++ config.map(value => MaterialDeliveryConfiguration.configurationKey.name -> value.noSpaces)
  def prepareDatabase(database: Database): Unit = database.statement("CREATE TABLE " + quote(deliveryTable) + " (" + columns.map(name =>
    quote(name) + (if (name == revisionName) " BIGINT NOT NULL" else " TEXT") + (if (name == "id") " PRIMARY KEY" else "")).mkString(",") + ")")
  def documents(database: Database): Vector[Json] = database.connection { connection =>
    Using.resource(connection.createStatement()) { statement =>
      Using.resource(statement.executeQuery("SELECT document FROM " + quote(deliveryTable) + " ORDER BY id")) { rows =>
        val result = Vector.newBuilder[Json]
        while (rows.next()) result += parser.parse(rows.getString(1)).toOption.get
        result.result()
      }
    }
  }
  def record(value: org.goldenport.Consequence[org.goldenport.protocol.operation.OperationResponse]): Record = take(value) match {
    case org.goldenport.protocol.operation.OperationResponse.RecordResponse(result) => result
    case other => throw new AssertionError("Expected a native delivery record")
  }
  def fields: Vector[(String, Any)] = Vector("requestId" -> requestId, "contribution" -> MaterialRegistrationOperationsFixture.contribution)
  def upload(reference: String, index: Int): Vector[(String, Any)] = Vector("deliveryRef" -> reference,
    "resourceId" -> Vector("cover", "detail")(index), "content" -> MimeBody(ContentType.IMAGE_PNG, Bag.binary(pngs(index).toArray)),
    "content.filename" -> "original.png")

  /** Independent committed delivery-row reads precede every controlled provider write. */
  final class CheckpointPeer(database: Database, val delegate: MaterialRegistrationOperationsFixture.HttpPeer,
    beforeGet: String => Unit = _ => (), afterGet: String => Unit = _ => (),
    getOverride: String => Option[HttpResponse] = _ => None) extends HttpDriver {
    private var _observed = Vector.empty[Vector[Option[String]]]
    private var _documents = Vector.empty[Vector[Json]]
    def checkpoints: Vector[Vector[Json]] = _documents
    def observed: Vector[Vector[Option[String]]] = _observed
    private def _checkpoint(): Unit = {
      _documents :+= documents(database)
      _observed :+= documents(database).map(_.hcursor.get[Option[String]]("busy").toOption.get)
    }
    def get(path: String, headers: Map[String, String], properties: Vector[Property]): HttpResponse = {
      _checkpoint()
      beforeGet(path)
      val response = getOverride(path).getOrElse(delegate.get(path, headers, properties))
      afterGet(path)
      response
    }
    def post(path: String, body: Option[String], headers: Map[String, String], properties: Vector[Property]): HttpResponse = {
      _checkpoint(); delegate.post(path, body, headers, properties)
    }
    def put(path: String, body: Option[String], headers: Map[String, String], properties: Vector[Property]): HttpResponse = {
      _checkpoint(); delegate.put(path, body, headers, properties)
    }
    override def putBag(path: String, body: Option[Bag], headers: Map[String, String], properties: Vector[Property]): HttpResponse = {
      _checkpoint(); delegate.putBag(path, body, headers, properties)
    }
  }
}
