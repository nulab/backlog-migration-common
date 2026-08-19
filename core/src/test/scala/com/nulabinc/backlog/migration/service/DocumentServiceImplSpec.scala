package com.nulabinc.backlog.migration.service

import com.google.inject.Guice
import com.nulabinc.backlog.migration.common.conf.BacklogApiConfiguration
import com.nulabinc.backlog.migration.common.domain.{
  BacklogDocument,
  BacklogDocumentComment,
  BacklogDocumentCommentReply
}
import com.nulabinc.backlog.migration.common.modules.DefaultModule
import com.nulabinc.backlog.migration.common.service.DocumentServiceImpl
import com.nulabinc.backlog.migration.{SimpleFixture, TestPropertyResolver}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import spray.json._

/**
 * @author
 *   nulab
 */
class DocumentServiceImplSpec extends AnyFlatSpec with Matchers with SimpleFixture {

  def documentService(): DocumentServiceImpl =
    Guice
      .createInjector(
        new DefaultModule(BacklogApiConfiguration("url", "key", "projectKey"))
      )
      .getInstance(classOf[DocumentServiceImpl])

  private val documentCreated = "2015-05-01T16:01:51+09:00"
  private val documentUpdated = "2015-05-02T16:01:51+09:00"

  private val document = BacklogDocument(
    optId = None,
    projectId = projectId,
    title = "document title",
    optJson = Some("""{"type":"doc","content":[]}"""),
    optPlain = Some("plain text"),
    optEmoji = Some(":smile:"),
    tags = Seq.empty,
    attachments = Seq.empty,
    comments = Seq.empty,
    optCreatedUser = Some(user1),
    optCreated = Some(documentCreated),
    optUpdatedUser = Some(user2),
    optUpdated = Some(documentUpdated)
  )

  "createDocumentJson" should "build the import request body" in {
    val propertyResolver = new TestPropertyResolver()

    val json = documentService().createDocumentJson(
      projectId,
      document,
      Some("parentDocumentId"),
      addLast = true,
      propertyResolver
    )

    json.fields("projectId") should be(JsNumber(projectId))
    json.fields("title") should be(JsString("document title"))
    json.fields("emoji") should be(JsString(":smile:"))
    json.fields("parentId") should be(JsString("parentDocumentId"))
    json.fields("addLast") should be(JsBoolean(true))
    json.fields("created") should be(JsString(documentCreated))
    json.fields("createdUserId") should be(JsNumber(userId1))
    json.fields("updated") should be(JsString(documentUpdated))
    json.fields("updatedUserId") should be(JsNumber(userId2))
    json.fields.keySet should not contain "content"
  }

  it should "omit optional fields that are not set" in {
    val propertyResolver = new TestPropertyResolver()
    val minimalDocument = document.copy(
      optEmoji = None,
      optCreatedUser = None,
      optCreated = None,
      optUpdatedUser = None,
      optUpdated = None
    )

    val json = documentService().createDocumentJson(
      projectId,
      minimalDocument,
      None,
      addLast = false,
      propertyResolver
    )

    json.fields.keySet should contain theSameElementsAs Set("projectId", "title", "addLast")
  }

  "updateContentJson" should "build the content import request body" in {
    val propertyResolver = new TestPropertyResolver()

    val json = documentService().updateContentJson(document, propertyResolver)

    json.fields("json") should be("""{"type":"doc","content":[]}""".parseJson)
    json.fields("plain") should be(JsString("plain text"))
    json.fields("updated") should be(JsString(documentUpdated))
    json.fields("updatedUserId") should be(JsNumber(userId2))
    json.fields.keySet should not contain "title"
  }

  it should "fall back to an empty object and empty string when content is missing" in {
    val propertyResolver = new TestPropertyResolver()
    val emptyDocument    = document.copy(optJson = None, optPlain = None)

    val json = documentService().updateContentJson(emptyDocument, propertyResolver)

    json.fields("json") should be(JsObject.empty)
    json.fields("plain") should be(JsString(""))
  }

  "commentJson" should "build the comment import request body with replies" in {
    val propertyResolver = new TestPropertyResolver()
    val reply = BacklogDocumentCommentReply(
      optId = None,
      content = "reply content",
      plain = "reply plain",
      optCreatedUser = Some(user3),
      optCreated = Some(documentCreated),
      optUpdated = None
    )
    val comment = BacklogDocumentComment(
      optId = None,
      statusId = 1,
      content = "comment content",
      plain = "comment plain",
      commentType = "comment",
      optCreatedUser = Some(user1),
      optCreated = Some(documentCreated),
      optUpdated = Some(documentUpdated),
      replies = Seq(reply)
    )

    val json = documentService().commentJson(comment, propertyResolver)

    json.fields("content") should be(JsString("comment content"))
    json.fields("plain") should be(JsString("comment plain"))
    json.fields("statusId") should be(JsNumber(1))
    json.fields("commentType") should be(JsString("comment"))
    json.fields("createdUserId") should be(JsNumber(userId1))

    val replies = json.fields("replies").asInstanceOf[JsArray].elements
    replies should have size 1
    val replyJson = replies.head.asJsObject
    replyJson.fields("content") should be(JsString("reply content"))
    replyJson.fields("plain") should be(JsString("reply plain"))
    replyJson.fields("createdUserId") should be(JsNumber(userId3))
    replyJson.fields.keySet should not contain "updatedUserId"
  }

  it should "omit the replies field when there are no replies" in {
    val propertyResolver = new TestPropertyResolver()
    val comment = BacklogDocumentComment(
      optId = None,
      statusId = 1,
      content = "comment content",
      plain = "comment plain",
      commentType = "comment",
      optCreatedUser = None,
      optCreated = None,
      optUpdated = None,
      replies = Seq.empty
    )

    val json = documentService().commentJson(comment, propertyResolver)

    json.fields.keySet should not contain "replies"
  }

  "rewriteInlineCommentIds" should "rewrite every inlineComment mark's id using the mapping" in {
    val body =
      """{"type":"doc","content":[
        |{"type":"paragraph","content":[{"type":"text","text":"test","marks":[
        |{"type":"inlineComment","attrs":{"comment":{"id":"old-1","statusId":0}}}
        |]}]},
        |{"type":"paragraph","content":[{"type":"text","text":"foo","marks":[
        |{"type":"inlineComment","attrs":{"comment":{"id":"old-2","statusId":0}}}
        |]}]}
        |]}""".stripMargin
    val documentWithBody = document.copy(optJson = Some(body))
    val commentIdMap     = Map("old-1" -> "new-1", "old-2" -> "new-2")

    val rewritten = documentService().rewriteInlineCommentIds(documentWithBody, commentIdMap)

    val marks = rewritten.optJson.get.parseJson.asJsObject
      .fields("content")
      .asInstanceOf[JsArray]
      .elements
      .flatMap(_.asJsObject.fields("content").asInstanceOf[JsArray].elements)
      .flatMap(_.asJsObject.fields("marks").asInstanceOf[JsArray].elements)
      .map(_.asJsObject.fields("attrs").asJsObject.fields("comment").asJsObject.fields("id"))

    marks should contain theSameElementsInOrderAs Seq(JsString("new-1"), JsString("new-2"))
  }

  it should "leave the id untouched when no mapping exists for it" in {
    val body =
      """{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"test",
        |"marks":[{"type":"inlineComment","attrs":{"comment":{"id":"unmapped","statusId":0}}}]}]}]}""".stripMargin
    val documentWithBody = document.copy(optJson = Some(body))

    val rewritten = documentService().rewriteInlineCommentIds(documentWithBody, Map.empty)

    rewritten.optJson should be(Some(body))
  }

  it should "not touch marks other than inlineComment" in {
    val body =
      """{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"test",
        |"marks":[{"type":"bold"}]}]}]}""".stripMargin
    val documentWithBody = document.copy(optJson = Some(body))

    val rewritten =
      documentService().rewriteInlineCommentIds(documentWithBody, Map("old" -> "new"))

    rewritten.optJson.get.parseJson should be(body.parseJson)
  }

}
