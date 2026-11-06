package org.simplemodeling.textus.knowledgelake.material

import org.goldenport.bag.Bag
import org.goldenport.cncf.filestore.*
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.datatype.MimeType
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/** Capability-based direct delivery and explicit server-mediated selection.
  *
  * @since Oct. 25, 2026
  * @version Nov.  6, 2026
  */
final class MaterialDeliveryRouteSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "TKL route selection" should {
    "prefer the optional direct capability and retain an explicit reason for server-mediated transfer" in {
      Given("a native direct-capable provider and a provider exposing only the original FileStore contract")
      val provider = GoogleDriveFileStore.createC("drive", GoogleDriveCredentials.accessTokenC("synthetic-token").TAKE).TAKE
      val basic = new FileStore {
        def name: String = provider.name
        def stat(reference: FileStoreRef): ExecUowM[FileStoreMetadata] = provider.stat(reference)
        def read(reference: FileStoreRef): ExecUowM[FileStoreContent] = provider.read(reference)
        def createCollection(parent: FileStoreRef, name: String): ExecUowM[FileStoreMetadata] = provider.createCollection(parent, name)
        def create(parent: FileStoreRef, name: String, mediaType: MimeType, content: Bag): ExecUowM[FileStoreMetadata] = provider.create(parent, name, mediaType, content)
      }
      val olddirect = new DirectUploadFileStore {
        def name: String = basic.name
        def directUploadProtocol: String = "googleDriveResumable"
        def stat(reference: FileStoreRef): ExecUowM[FileStoreMetadata] = basic.stat(reference)
        def read(reference: FileStoreRef): ExecUowM[FileStoreContent] = basic.read(reference)
        def createCollection(parent: FileStoreRef, name: String): ExecUowM[FileStoreMetadata] = basic.createCollection(parent, name)
        def create(parent: FileStoreRef, name: String, mediaType: MimeType, content: Bag): ExecUowM[FileStoreMetadata] = basic.create(parent, name, mediaType, content)
        def prepareDirectUpload(parent: FileStoreRef, name: String, mediaType: MimeType, byteLength: Long): ExecUowM[DirectFileUpload] =
          fail("The old direct capability must never be selected")
        def inspectDirectUpload(upload: DirectFileUpload): ExecUowM[DirectFileUploadStatus] =
          fail("The old direct capability must never be selected")
      }
      When("the operator permits direct preference or explicitly selects server transfer")
      val direct = MaterialDeliveryRoute.select(provider, true)
      val fallback = MaterialDeliveryRoute.select(basic, true)
      val selected = MaterialDeliveryRoute.select(provider, false)
      Then("capability absence and an explicit server policy are distinguishable without issuing any transfer")
      direct shouldBe MaterialDeliveryRoute("googleDriveResumable", None)
      MaterialDeliveryRoute.select(olddirect, true) shouldBe MaterialDeliveryRoute("tklMultipart", Some("providerDirectUploadUnsupported"))
      fallback shouldBe MaterialDeliveryRoute("tklMultipart", Some("providerDirectUploadUnsupported"))
      selected shouldBe MaterialDeliveryRoute("tklMultipart", Some("operatorSelectedServerTransfer"))
    }
  }
}
