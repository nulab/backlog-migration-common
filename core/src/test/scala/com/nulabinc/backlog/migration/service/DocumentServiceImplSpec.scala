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
      isTrash = false,
      propertyResolver
    )

    json.fields("projectId") should be(JsNumber(projectId))
    json.fields("title") should be(JsString("document title"))
    json.fields("emoji") should be(JsString(":smile:"))
    json.fields("parentId") should be(JsString("parentDocumentId"))
    json.fields("addLast") should be(JsBoolean(true))
    json.fields("isTrash") should be(JsBoolean(false))
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
      isTrash = true,
      propertyResolver
    )

    json.fields.keySet should contain theSameElementsAs Set("projectId", "title", "addLast", "isTrash")
    json.fields("isTrash") should be(JsBoolean(true))
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

  "rewriteIssueMentions" should "rewrite a same-project issue mention that has issueId and projectId" in {
    val body =
      """{"type":"doc","content":[{"type":"issueMention","attrs":{
        |"id":"SRC-85","label":"emoji test","mentionType":"inline",
        |"projectKey":"SRC","projectId":100,"issueId":200}}]}""".stripMargin
    val documentWithBody = document.copy(optJson = Some(body))

    val rewritten = documentService().rewriteIssueMentions(
      documentWithBody,
      issueIdMap = Map(200L -> 201L),
      issueKeyMap = Map("SRC-85" -> "DST-1"),
      srcProjectId = 100L,
      srcProjectKey = "SRC",
      dstProjectId = 101L,
      dstProjectKey = "DST"
    )

    val attrs = rewritten.optJson.get.parseJson.asJsObject
      .fields("content")
      .asInstanceOf[JsArray]
      .elements
      .head
      .asJsObject
      .fields("attrs")
      .asJsObject

    attrs.fields("id") should be(JsString("DST-1"))
    attrs.fields("issueId") should be(JsNumber(201))
    attrs.fields("projectId") should be(JsNumber(101))
    attrs.fields("projectKey") should be(JsString("DST"))
    attrs.fields("label") should be(JsString("emoji test"))
    attrs.fields("mentionType") should be(JsString("inline"))
  }

  it should "rewrite a same-project issue mention missing issueId/projectId via key-only fallback" in {
    val body =
      """{"type":"doc","content":[{"type":"issueMention","attrs":{
        |"id":"SRC-84","label":"label missing ids","mentionType":"inline",
        |"projectKey":"SRC"}}]}""".stripMargin
    val documentWithBody = document.copy(optJson = Some(body))

    val rewritten = documentService().rewriteIssueMentions(
      documentWithBody,
      issueIdMap = Map.empty,
      issueKeyMap = Map("SRC-84" -> "DST-2"),
      srcProjectId = 100L,
      srcProjectKey = "SRC",
      dstProjectId = 101L,
      dstProjectKey = "DST"
    )

    val attrs = rewritten.optJson.get.parseJson.asJsObject
      .fields("content")
      .asInstanceOf[JsArray]
      .elements
      .head
      .asJsObject
      .fields("attrs")
      .asJsObject

    attrs.fields("id") should be(JsString("DST-2"))
    attrs.fields("projectKey") should be(JsString("DST"))
    attrs.fields.keySet should not contain "issueId"
    attrs.fields.keySet should not contain "projectId"
  }

  it should "leave a same-project mention untouched when the issue isn't in either map" in {
    val body =
      """{"type":"doc","content":[{"type":"issueMention","attrs":{
        |"id":"SRC-99","label":"unmigrated","mentionType":"inline",
        |"projectKey":"SRC","projectId":100,"issueId":999}}]}""".stripMargin
    val documentWithBody = document.copy(optJson = Some(body))

    val rewritten = documentService().rewriteIssueMentions(
      documentWithBody,
      issueIdMap = Map(200L -> 201L),
      issueKeyMap = Map("SRC-85" -> "DST-1"),
      srcProjectId = 100L,
      srcProjectKey = "SRC",
      dstProjectId = 101L,
      dstProjectKey = "DST"
    )

    rewritten.optJson.get.parseJson should be(body.parseJson)
  }

  it should "leave a mention pointing at a different project completely untouched" in {
    val body =
      """{"type":"doc","content":[{"type":"issueMention","attrs":{
        |"id":"OTHER-1","label":"other project issue","mentionType":"inline",
        |"projectKey":"OTHER","projectId":300,"issueId":400}}]}""".stripMargin
    val documentWithBody = document.copy(optJson = Some(body))

    val rewritten = documentService().rewriteIssueMentions(
      documentWithBody,
      issueIdMap = Map(400L -> 401L),
      issueKeyMap = Map("OTHER-1" -> "DST-9"),
      srcProjectId = 100L,
      srcProjectKey = "SRC",
      dstProjectId = 101L,
      dstProjectKey = "DST"
    )

    rewritten.optJson.get.parseJson should be(body.parseJson)
  }

  it should "return the document unchanged (no parse round-trip) when both maps are empty" in {
    val body =
      """{"type":"doc","content":[{"type":"issueMention","attrs":{
        |"id":"SRC-85","label":"emoji test","mentionType":"inline",
        |"projectKey":"SRC","projectId":100,"issueId":200}}]}""".stripMargin
    val documentWithBody = document.copy(optJson = Some(body))

    val rewritten = documentService().rewriteIssueMentions(
      documentWithBody,
      issueIdMap = Map.empty,
      issueKeyMap = Map.empty,
      srcProjectId = 100L,
      srcProjectKey = "SRC",
      dstProjectId = 101L,
      dstProjectKey = "DST"
    )

    rewritten.optJson should be theSameInstanceAs documentWithBody.optJson
  }

  it should "rewrite the matching bracket tag in optPlain for a fully-resolved same-project mention" in {
    val body =
      """{"type":"doc","content":[{"type":"issueMention","attrs":{
        |"id":"SRC-85","label":"emoji test","mentionType":"inline",
        |"projectKey":"SRC","projectId":100,"issueId":200}}]}""".stripMargin
    val oldTag = """[issueMention id="SRC-85" label="emoji test" mentionType="inline" projectKey="SRC" projectId="100" issueId="200"]"""
    val newTag = """[issueMention id="DST-1" label="emoji test" mentionType="inline" projectKey="DST" projectId="101" issueId="201"]"""
    val documentWithBody =
      document.copy(optJson = Some(body), optPlain = Some(s"before $oldTag after"))

    val rewritten = documentService().rewriteIssueMentions(
      documentWithBody,
      issueIdMap = Map(200L -> 201L),
      issueKeyMap = Map("SRC-85" -> "DST-1"),
      srcProjectId = 100L,
      srcProjectKey = "SRC",
      dstProjectId = 101L,
      dstProjectKey = "DST"
    )

    rewritten.optPlain should be(Some(s"before $newTag after"))
  }

  it should "rewrite the plain-text tag via key-only fallback without gaining issueId/projectId" in {
    val body =
      """{"type":"doc","content":[{"type":"issueMention","attrs":{
        |"id":"SRC-84","label":"label missing ids","mentionType":"inline",
        |"projectKey":"SRC"}}]}""".stripMargin
    val oldTag = """[issueMention id="SRC-84" label="label missing ids" mentionType="inline" projectKey="SRC"]"""
    val newTag = """[issueMention id="DST-2" label="label missing ids" mentionType="inline" projectKey="DST"]"""
    val documentWithBody =
      document.copy(optJson = Some(body), optPlain = Some(s"text $oldTag more"))

    val rewritten = documentService().rewriteIssueMentions(
      documentWithBody,
      issueIdMap = Map.empty,
      issueKeyMap = Map("SRC-84" -> "DST-2"),
      srcProjectId = 100L,
      srcProjectKey = "SRC",
      dstProjectId = 101L,
      dstProjectKey = "DST"
    )

    rewritten.optPlain should be(Some(s"text $newTag more"))
  }

  it should "leave optPlain unchanged and not throw when the expected old tag text can't be found" in {
    val body =
      """{"type":"doc","content":[{"type":"issueMention","attrs":{
        |"id":"SRC-85","label":"emoji test","mentionType":"inline",
        |"projectKey":"SRC","projectId":100,"issueId":200}}]}""".stripMargin
    val drifitngPlain = "this plain text has drifted and no longer contains the mention tag"
    val documentWithBody =
      document.copy(optJson = Some(body), optPlain = Some(drifitngPlain))

    val rewritten = documentService().rewriteIssueMentions(
      documentWithBody,
      issueIdMap = Map(200L -> 201L),
      issueKeyMap = Map("SRC-85" -> "DST-1"),
      srcProjectId = 100L,
      srcProjectKey = "SRC",
      dstProjectId = 101L,
      dstProjectKey = "DST"
    )

    rewritten.optPlain should be(Some(drifitngPlain))
  }

  it should "rewrite every issueMention tag in optPlain across label edge cases " +
    "(missing ids, brackets, encoded quotes)" in {
      val jsonBody =
        """{"type":"doc","content":[{"type":"paragraph"},{"type":"paragraph","content":[{"type":"text","text":"text one"}]},{"type":"paragraph","content":[{"type":"issueMention","attrs":{"id":"SRCPROJ-1","label":"label one","mentionType":"inline","projectKey":"SRCPROJ","projectId":1000,"issueId":100001}},{"type":"text","text":" "}]},{"type":"paragraph"},{"type":"paragraph","content":[{"type":"text","text":"text two"}]},{"type":"paragraph","content":[{"type":"issueMention","attrs":{"id":"SRCPROJ-2","label":"label missing ids","mentionType":"inline","projectKey":"SRCPROJ"}},{"type":"text","text":" "}]},{"type":"paragraph"},{"type":"paragraph","content":[{"type":"text","text":"“stray quote“and[escaped]"}]},{"type":"paragraph","content":[{"type":"issueMention","attrs":{"id":"SRCPROJ-3","label":"label with &quot;quote&quot; and [brackets]","mentionType":"inline","projectKey":"SRCPROJ","projectId":1000,"issueId":100003}},{"type":"text","text":" "}]},{"type":"paragraph"}]}"""

      val oldTag1 =
        """[issueMention id="SRCPROJ-1" label="label one" mentionType="inline" projectKey="SRCPROJ" projectId="1000" issueId="100001"]"""
      val oldTag2 =
        """[issueMention id="SRCPROJ-2" label="label missing ids" mentionType="inline" projectKey="SRCPROJ"]"""
      val oldTag3 =
        """[issueMention id="SRCPROJ-3" label="label with &quot;quote&quot; and [brackets]" mentionType="inline" projectKey="SRCPROJ" projectId="1000" issueId="100003"]"""

      val filler1 = "\n\ntext one\n\n"
      val filler2 = " \n\n\n\ntext two\n\n"
      val filler3 = " \n\n\n\n“stray quote“and\\[escaped\\]\n\n"
      val filler4 = " \n\n"

      val plainBody =
        filler1 + oldTag1 + filler2 + oldTag2 + filler3 + oldTag3 + filler4

      val documentWithBody = document.copy(optJson = Some(jsonBody), optPlain = Some(plainBody))

      val issueIdMap = Map(
        100001L -> 200001L,
        100003L -> 200003L
      )
      val issueKeyMap = Map(
        "SRCPROJ-1" -> "DST-1",
        "SRCPROJ-2" -> "DST-2",
        "SRCPROJ-3" -> "DST-3"
      )

      val rewritten = documentService().rewriteIssueMentions(
        documentWithBody,
        issueIdMap = issueIdMap,
        issueKeyMap = issueKeyMap,
        srcProjectId = 1000L,
        srcProjectKey = "SRCPROJ",
        dstProjectId = 2000L,
        dstProjectKey = "DSTPROJ"
      )

      val newTag1 =
        """[issueMention id="DST-1" label="label one" mentionType="inline" projectKey="DSTPROJ" projectId="2000" issueId="200001"]"""
      val newTag2 =
        """[issueMention id="DST-2" label="label missing ids" mentionType="inline" projectKey="DSTPROJ"]"""
      val newTag3 =
        """[issueMention id="DST-3" label="label with &quot;quote&quot; and [brackets]" mentionType="inline" projectKey="DSTPROJ" projectId="2000" issueId="200003"]"""

      val expectedPlainBody =
        filler1 + newTag1 + filler2 + newTag2 + filler3 + newTag3 + filler4

      rewritten.optPlain should be(Some(expectedPlainBody))

      def collectIssueMentionIds(value: JsValue): Seq[String] = value match {
        case obj: JsObject =>
          val here = obj.fields.get("type") match {
            case Some(JsString("issueMention")) =>
              obj.fields.get("attrs").toSeq.collect { case attrs: JsObject =>
                attrs.fields("id")
              }.collect { case JsString(id) => id }
            case _ => Seq.empty
          }
          here ++ obj.fields.values.flatMap(collectIssueMentionIds)
        case JsArray(elements) => elements.flatMap(collectIssueMentionIds)
        case _                 => Seq.empty
      }

      collectIssueMentionIds(rewritten.optJson.get.parseJson) should contain theSameElementsInOrderAs Seq(
        "DST-1",
        "DST-2",
        "DST-3"
      )
    }

}
