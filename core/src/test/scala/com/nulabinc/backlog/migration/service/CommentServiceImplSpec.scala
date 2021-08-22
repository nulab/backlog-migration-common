package com.nulabinc.backlog.migration.service

import com.google.inject.Guice
import com.google.inject.util.Modules
import com.nulabinc.backlog.migration.common.client.params.ImportUpdateIssueParams
import com.nulabinc.backlog.migration.common.conf.BacklogApiConfiguration
import com.nulabinc.backlog.migration.common.dsl.ConsoleDSL
import com.nulabinc.backlog.migration.common.interpreters.JansiConsoleDSL
import com.nulabinc.backlog.migration.common.modules.DefaultModule
import com.nulabinc.backlog.migration.common.service.CommentServiceImpl
import com.nulabinc.backlog.migration.{SimpleFixture, TestModule, TestPropertyResolver}
import monix.eval.Task
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters._

/**
 * @author
 *   uchida
 */
class CommentServiceImplSpec extends AnyFlatSpec with Matchers with SimpleFixture {

  implicit val consoleDSL: ConsoleDSL[Task] = JansiConsoleDSL()

  def commentService() = {
    Guice
      .createInjector(
        Modules
          .`override`(
            new DefaultModule(
              BacklogApiConfiguration("url", "key", "projectKey")
            )
          )
          .`with`(new TestModule())
      )
      .getInstance(classOf[CommentServiceImpl])
  }

  "setUpdateParam" should "return the valid params" in {
    val propertyResolver = new TestPropertyResolver()
    val toRemoteIssueId  = (_: Long) => Some(1): Option[Long]
    val postAttachment   = (_: String) => None: Option[Long]

    val params = commentService().setUpdateParam(
      issueId1,
      propertyResolver,
      toRemoteIssueId,
      postAttachment
    )(comment1)
    getValue(params, "comment").map(_.trim) should be(Some(commentContent))
    getValues(params, "notifiedUserId[]").map(_.toLong) should be(Seq(userId3))
    getValue(params, "created") should be(Some(commentCreated))
    getValue(params, "updated") should be(Some(commentCreated))
    getValue(params, "updatedUserId").map(_.toLong) should be(Some(userId1))
    getValue(params, "summary") should be(summaryChangeLog.optNewValue)
    getValue(params, "description") should be(descriptionChangeLog.optNewValue)
    getValues(params, "categoryId[]").map(
      _.toLong
    ) should contain theSameElementsAs (Seq(categoryId1, categoryId2))
    getValues(params, "versionId[]").map(
      _.toLong
    ) should contain theSameElementsAs (Seq(versionId1, versionId2))
    getValues(params, "milestoneId[]").map(
      _.toLong
    ) should contain theSameElementsAs (Seq(versionId3, versionId4))
    getValue(params, "statusId").map(_.toInt) should be(Some(statusId1 - 1))
    getValue(params, "assigneeId").map(_.toInt) should be(Some(userId2))
    //getValue(params, "issueTypeId").map(_.toInt) should be(Some(issueTypeId))
    getValue(params, "startDate") should be(Some(startDate))
    getValue(params, "dueDate") should be(Some(dueDate))
    getValue(params, "priorityId").map(_.toInt) should be(Some(priorityId))
    getValue(params, "resolutionId").map(_.toInt) should be(Some(resolutionId))
    getValue(params, "estimatedHours").map(_.toFloat) should be(
      Some(estimatedHours)
    )
    getValue(params, "actualHours").map(_.toFloat) should be(Some(actualHours))
    getValue(params, "parentIssueId").map(_.toInt) should be(Some(1))
    getValue(params, s"customField_${textCustomFieldId}") should be(
      Some(textCustomFieldValue)
    )
    getValue(params, s"customField_${textAreaCustomFieldId}") should be(
      Some(textAreaCustomFieldValue)
    )
    getValue(params, s"customField_${numericCustomFieldId}") should be(
      Some(numericCustomFieldValue)
    )
    getValue(params, s"customField_${dateCustomFieldId}") should be(
      Some(dateCustomFieldValue)
    )

    getValue(params, s"customField_${singleListCustomFieldId}").map(_.toInt) should be(item1.optId)
    getValues(params, s"customField_${multipleListCustomFieldId}")
      .map(_.toInt) should contain theSameElementsAs Seq(
      item1.optId,
      item2.optId
    ).flatten.map(_.toInt)
    getValues(params, s"customField_${checkBoxCustomFieldId}").map(
      _.toInt
    ) should contain theSameElementsAs (Seq(item1.optId, item2.optId).flatten.map(_.toInt))
    getValue(params, s"customField_${radioCustomFieldId}").map(_.toInt) should be(item1.optId)
  }

  "setUpdateParam(Reset)" should "return the valid params" in {
    val propertyResolver = new TestPropertyResolver()
    val toRemoteIssueId  = (_: Long) => Some(1): Option[Long]
    val postAttachment   = (_: String) => None: Option[Long]

    val params = commentService().setUpdateParam(
      issueId1,
      propertyResolver,
      toRemoteIssueId,
      postAttachment
    )(comment2)
    getValues(params, "categoryId[]") should contain theSameElementsAs (Seq(""))
    getValues(params, "versionId[]") should contain theSameElementsAs (Seq(""))
    getValues(params, "milestoneId[]") should contain theSameElementsAs (Seq(
      ""
    ))
    getValue(params, "assigneeId") should be(Some(""))
    getValue(params, "startDate") should be(Some(""))
    getValue(params, "dueDate") should be(Some(""))
    getValue(params, "estimatedHours") should be(Some(""))
    getValue(params, "actualHours") should be(Some(""))
    getValue(params, "parentIssueId") should be(Some(""))
  }

  private[this] def getValue(
      params: ImportUpdateIssueParams,
      name: String
  ): Option[String] = {
    params.getParamList.asScala.find(p => p.getName == name).map(_.getValue)
  }

  private[this] def getValues(
      params: ImportUpdateIssueParams,
      name: String
  ): Seq[String] = {
    params.getParamList.asScala.toSeq.filter(p => p.getName == name).map(_.getValue)
  }

}
