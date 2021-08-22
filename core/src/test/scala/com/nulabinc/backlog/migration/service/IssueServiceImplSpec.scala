package com.nulabinc.backlog.migration.service

import com.google.inject.Guice
import com.nulabinc.backlog.migration.common.client.params.ImportIssueParams
import com.nulabinc.backlog.migration.common.conf.BacklogApiConfiguration
import com.nulabinc.backlog.migration.common.modules.DefaultModule
import com.nulabinc.backlog.migration.common.service.IssueServiceImpl
import com.nulabinc.backlog.migration.{SimpleFixture, TestPropertyResolver}
import com.nulabinc.backlog4j.Issue.PriorityType
import com.nulabinc.backlog4j.api.option.{GetIssuesCountParams, GetIssuesParams}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters._

/**
 * @author
 *   uchida
 */
class IssueServiceImplSpec extends AnyFlatSpec with Matchers with SimpleFixture {

  def issueService() = {
    Guice
      .createInjector(
        new DefaultModule(BacklogApiConfiguration("url", "key", "projectKey"))
      )
      .getInstance(classOf[IssueServiceImpl])
  }

  "setCreateParam" should "return the valid params" in {
    val propertyResolver = new TestPropertyResolver()
    val toRemoteIssueId  = (_: Long) => None: Option[Long]
    val issueOfId        = (_: Long) => issue2
    val postAttachment   = (_: String) => None: Option[Long]

    val params = issueService().setCreateParam(
      projectId,
      propertyResolver,
      toRemoteIssueId,
      postAttachment,
      issueOfId
    )(issue1)
    getValue(params, "projectId").map(_.toInt) should be(Some(projectId))
    getValue(params, "summary") should be(Some(summary))
    getValue(params, "issueTypeId").map(_.toInt) should be(Some(issueTypeId))
    getValue(params, "priorityId").map(_.toInt) should be(
      Some(PriorityType.Normal.getIntValue)
    )
    getValue(params, "description") should be(Some(description))
    getValue(params, "startDate") should be(Some(startDate))
    getValue(params, "dueDate") should be(Some(dueDate))
    getValue(params, "estimatedHours").map(_.toFloat) should be(
      Some(estimatedHours)
    )
    getValue(params, "actualHours").map(_.toFloat) should be(Some(actualHours))
    getValues(params, "categoryId[]").map(
      _.toLong
    ) should contain theSameElementsAs (Seq(categoryId1, categoryId2))
    getValues(params, "versionId[]").map(
      _.toLong
    ) should contain theSameElementsAs (Seq(versionId1, versionId2))
    getValues(params, "milestoneId[]").map(
      _.toLong
    ) should contain theSameElementsAs (Seq(versionId3, versionId4))
    getValue(params, "assigneeId").map(_.toInt) should be(Some(userId1))
    getValues(params, "notifiedUserId[]").map(_.toLong) should be(Seq(userId3))
    getValue(params, "createdUserId").map(_.toLong) should be(Some(userId2))
    getValue(params, "created") should be(Some(issueCreated))
    getValue(params, "updatedUserId").map(_.toLong) should be(Some(userId2))
    getValue(params, "updated") should be(Some(issueUpdated))
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
    getValue(params, s"customField_${singleListCustomFieldId}") should be(
      item1.optId.map(_.toString)
    )
    getValues(
      params,
      s"customField_${multipleListCustomFieldId}"
    ) should contain theSameElementsAs (Seq(item1.optId, item2.optId).flatten.map(_.toString))
    getValues(
      params,
      s"customField_${checkBoxCustomFieldId}"
    ) should contain theSameElementsAs (Seq(item1.optId, item2.optId).flatten.map(_.toString))
    getValue(params, s"customField_${radioCustomFieldId}") should be(
      item1.optId.map(_.toString)
    )
  }

  "addIssuesParams" should "return the valid params" in {
    val filter = new StringBuilder
    filter.append(s"projectId[]=${projectId}")
    filter.append(s"&categoryId[]=${categoryId1}")
    filter.append(s"&versionId[]=${versionId1}")
    filter.append(s"&milestoneId[]=${versionId2}")
    filter.append(s"&statusId[]=${statusId}")
    filter.append(s"&priorityId[]=${priorityId}")
    filter.append(s"&assigneeId[]=${userId1}")
    filter.append(s"&createdUserId[]=${userId2}")
    filter.append(s"&resolutionId[]=${resolutionId}")
    filter.append(s"&parentChild=0")
    filter.append(s"&attachment=true")
    filter.append(s"&sharedFile=true")
    filter.append(s"&createdSince=2017-01-01")
    filter.append(s"&createdUntil=2017-02-01")
    filter.append(s"&updatedUntil=2017-03-01")
    filter.append(s"&startDateSince=2017-04-01")
    filter.append(s"&dueDateSince=2017-05-01")
    filter.append(s"&dueDateUntil=2017-06-01")
    filter.append(s"&id[]=${issueId1}")
    filter.append(s"&parentIssueId[]=${issueId2}")
    filter.append(s"&keyword=test")

    val optFilter = Some(filter.toString())
    val params    = new GetIssuesParams(List(projectId).asJava)
    issueService().addIssuesParams(params, optFilter)
    getValue(params, "projectId[]").map(_.toInt) should be(Some(projectId))
    getValue(params, "categoryId[]").map(_.toInt) should be(Some(categoryId1))
    getValue(params, "versionId[]").map(_.toInt) should be(Some(versionId1))
    getValue(params, "milestoneId[]").map(_.toInt) should be(Some(versionId2))
    getValue(params, "statusId[]").map(_.toInt) should be(Some(statusId))
    getValue(params, "priorityId[]").map(_.toInt) should be(Some(priorityId))
    getValue(params, "assigneeId[]").map(_.toInt) should be(Some(userId1))
    getValue(params, "createdUserId[]").map(_.toInt) should be(Some(userId2))
    getValue(params, "resolutionId[]").map(_.toInt) should be(
      Some(resolutionId)
    )
    getValue(params, "parentChild").map(_.toInt) should be(
      Some(GetIssuesParams.ParentChildType.All.getIntValue)
    )
    getValue(params, "attachment") should be(Some("true"))
    getValue(params, "sharedFile") should be(Some("true"))
    getValue(params, "createdSince") should be(Some("2017-01-01"))
    getValue(params, "createdUntil") should be(Some("2017-02-01"))
    getValue(params, "updatedUntil") should be(Some("2017-03-01"))
    getValue(params, "startDateSince") should be(Some("2017-04-01"))
    getValue(params, "dueDateSince") should be(Some("2017-05-01"))
    getValue(params, "dueDateUntil") should be(Some("2017-06-01"))
    getValue(params, "id[]").map(_.toInt) should be(Some(issueId1))
    getValue(params, "parentIssueId[]").map(_.toInt) should be(Some(issueId2))
    getValue(params, "keyword") should be(Some("test"))
  }

  "addIssuesCountParams" should "return the valid params" in {
    val filter = new StringBuilder
    filter.append(s"projectId[]=${projectId}")
    filter.append(s"&categoryId[]=${categoryId1}")
    filter.append(s"&versionId[]=${versionId1}")
    filter.append(s"&milestoneId[]=${versionId2}")
    filter.append(s"&statusId[]=${statusId}")
    filter.append(s"&priorityId[]=${priorityId}")
    filter.append(s"&assigneeId[]=${userId1}")
    filter.append(s"&createdUserId[]=${userId2}")
    filter.append(s"&resolutionId[]=${resolutionId}")
    filter.append(s"&parentChild=0")
    filter.append(s"&attachment=true")
    filter.append(s"&sharedFile=true")
    filter.append(s"&createdSince=2017-01-01")
    filter.append(s"&createdUntil=2017-02-01")
    filter.append(s"&updatedUntil=2017-03-01")
    filter.append(s"&startDateSince=2017-04-01")
    filter.append(s"&dueDateSince=2017-05-01")
    filter.append(s"&dueDateUntil=2017-06-01")
    filter.append(s"&id[]=${issueId1}")
    filter.append(s"&parentIssueId[]=${issueId2}")
    filter.append(s"&keyword=test")

    val optFilter = Some(filter.toString())
    val params: GetIssuesCountParams =
      new GetIssuesCountParams(List(projectId).asJava)
    issueService().addIssuesCountParams(params, optFilter)
    getValue(params, "projectId[]").map(_.toInt) should be(Some(projectId))
    getValue(params, "categoryId[]").map(_.toInt) should be(Some(categoryId1))
    getValue(params, "versionId[]").map(_.toInt) should be(Some(versionId1))
    getValue(params, "milestoneId[]").map(_.toInt) should be(Some(versionId2))
    getValue(params, "statusId[]").map(_.toInt) should be(Some(statusId))
    getValue(params, "priorityId[]").map(_.toInt) should be(Some(priorityId))
    getValue(params, "assigneeId[]").map(_.toInt) should be(Some(userId1))
    getValue(params, "createdUserId[]").map(_.toInt) should be(Some(userId2))
    getValue(params, "resolutionId[]").map(_.toInt) should be(
      Some(resolutionId)
    )
    getValue(params, "parentChild").map(_.toInt) should be(
      Some(GetIssuesParams.ParentChildType.All.getIntValue)
    )
    getValue(params, "attachment") should be(Some("true"))
    getValue(params, "sharedFile") should be(Some("true"))
    getValue(params, "createdSince") should be(Some("2017-01-01"))
    getValue(params, "createdUntil") should be(Some("2017-02-01"))
    getValue(params, "updatedUntil") should be(Some("2017-03-01"))
    getValue(params, "startDateSince") should be(Some("2017-04-01"))
    getValue(params, "dueDateSince") should be(Some("2017-05-01"))
    getValue(params, "dueDateUntil") should be(Some("2017-06-01"))
    getValue(params, "id[]").map(_.toInt) should be(Some(issueId1))
    getValue(params, "parentIssueId[]").map(_.toInt) should be(Some(issueId2))
    getValue(params, "keyword") should be(Some("test"))
  }

  private[this] def getValue(
      params: GetIssuesCountParams,
      name: String
  ): Option[String] = {
    params.getParamList.asScala.find(p => p.getName == name).map(_.getValue)
  }

  private[this] def getValue(
      params: GetIssuesParams,
      name: String
  ): Option[String] = {
    params.getParamList.asScala.find(p => p.getName == name).map(_.getValue)
  }

  private[this] def getValue(
      params: ImportIssueParams,
      name: String
  ): Option[String] = {
    params.getParamList.asScala.find(p => p.getName == name).map(_.getValue)
  }

  private[this] def getValues(
      params: ImportIssueParams,
      name: String
  ): Seq[String] = {
    params.getParamList.asScala.toSeq.filter(p => p.getName == name).map(_.getValue)
  }

}
