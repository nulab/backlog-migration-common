package com.nulabinc.backlog.migration.service

import java.io.InputStream
import javax.inject.Inject

import com.netaporter.uri.Uri
import com.nulabinc.backlog.migration.converter.Backlog4jConverters
import com.nulabinc.backlog.migration.domain._
import com.nulabinc.backlog.migration.utils.{DateUtil, Logging}
import com.nulabinc.backlog4j.CustomField.FieldType
import com.nulabinc.backlog4j.Issue.PriorityType
import com.nulabinc.backlog4j._
import com.nulabinc.backlog4j.api.option.{CreateIssueParams, GetIssuesCountParams, GetIssuesParams, ImportIssueParams}
import com.osinka.i18n.Messages

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class IssueServiceImpl @Inject()(backlog: BacklogClient) extends IssueService with Logging {

  override def issueOfId(id: Long): BacklogIssue = Backlog4jConverters.Issue(backlog.getIssue(id))

  override def optIssueOfKey(key: String): Option[BacklogIssue] =
    try {
      Some(issueOfKey(key))
    } catch {
      case _: Throwable => None
    }

  override def issueOfKey(key: String): BacklogIssue = Backlog4jConverters.Issue(backlog.getIssue(key))

  override def optIssueOfParams(projectId: Long, backlogIssue: BacklogIssue): Option[BacklogIssue] = {
    val params: GetIssuesParams = new GetIssuesParams(List(projectId).asJava)
    for { created <- backlogIssue.operation.optCreated } yield {
      if (created.nonEmpty) {
        params.createdSince(DateUtil.isoToDateFormat(created))
      }
    }
    if (backlogIssue.originalSummary.nonEmpty) {
      params.keyword(backlogIssue.originalSummary)
    }
    val issues = try {
      backlog.getIssues(params).asScala
    } catch {
      case e: BacklogAPIException =>
        logger.error(e.getMessage, e)
        Seq.empty[Issue]
    }
    (for {
      createdUser  <- backlogIssue.operation.optCreatedUser
      userId       <- createdUser.optUserId
      createdSince <- backlogIssue.operation.optCreated
    } yield {
      val optFoundIssue = issues.find { issue =>
        (backlogIssue.originalSummary == issue.getSummary) && (userId.trim == issue.getCreatedUser.getUserId.trim) &&
        (createdSince == DateUtil.isoFormat(issue.getCreated))
      }
      optFoundIssue match {
        case Some(foundIssue) => Backlog4jConverters.Issue(foundIssue)
        case _                => None
      }
      optFoundIssue.map(Backlog4jConverters.Issue.apply)
    }).flatten
  }

  override def allIssues(projectId: Long, offset: Int, count: Int, filter: Option[String]): Seq[Issue] = {
    val params: GetIssuesParams = new GetIssuesParams(List(projectId).asJava)
    params.offset(offset.toLong)
    params.count(count)
    params.sort(GetIssuesParams.SortKey.Created)
    params.order(GetIssuesParams.Order.Asc)
    addIssuesParams(params, filter)
    try {
      backlog.getIssues(params).asScala
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        Seq.empty[Issue]
    }
  }

  override def countIssues(projectId: Long, filter: Option[String]): Int = {
    val params: GetIssuesCountParams = new GetIssuesCountParams(List(projectId).asJava)
    addIssuesCountParams(params, filter)
    try {
      backlog.getIssuesCount(params)
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        0
    }
  }

  override def downloadIssueAttachment(issueId: Long, attachmentId: Long): Option[(String, InputStream)] =
    try {
      val attachmentData = backlog.downloadIssueAttachment(issueId, attachmentId)
      Some((attachmentData.getFilename, attachmentData.getContent))
    } catch {
      case e: Throwable =>
        logger.warn(e.getMessage, e)
        None
    }

  override def exists(projectId: Long, backlogIssue: BacklogIssue): Boolean = {
    val params: GetIssuesParams = new GetIssuesParams(List(projectId).asJava)
    for { created <- backlogIssue.operation.optCreated } yield {
      if (created.nonEmpty) {
        params.createdSince(DateUtil.isoToDateFormat(created))
      }
    }
    if (backlogIssue.originalSummary.nonEmpty) {
      params.keyword(backlogIssue.originalSummary)
    }
    val issues = try {
      backlog.getIssues(params).asScala
    } catch {
      case e: BacklogAPIException =>
        logger.error(e.getMessage, e)
        Seq.empty[Issue]
    }
    (for {
      createdUser  <- backlogIssue.operation.optCreatedUser
      userId       <- createdUser.optUserId
      createdSince <- backlogIssue.operation.optCreated
    } yield {
      issues.exists { issue =>
        (backlogIssue.originalSummary == issue.getSummary) && (userId.trim == issue.getCreatedUser.getUserId.trim) &&
        (createdSince == DateUtil.isoFormat(issue.getCreated))
      }
    }).getOrElse(false)
  }

  def create(setCreateParam: BacklogIssue => ImportIssueParams)(backlogIssue: BacklogIssue): Either[Throwable, BacklogIssue] = {
    logger.debug(s"[Start Create Issue]:${backlogIssue.id}${backlogIssue.optIssueKey}----------------------------")
    val result = createIssue(setCreateParam(backlogIssue))
    result match {
      case Right(_) =>
        logger.debug(s"[Success Finish Create Issue]:${backlogIssue.id}${backlogIssue.optIssueKey}----------------------------")
      case Left(_) =>
        logger.debug(s"[Fail Finish Create Issue]:${backlogIssue.id}${backlogIssue.optIssueKey}----------------------------")
    }
    result
  }

  private[this] def createIssue(params: ImportIssueParams): Either[Throwable, BacklogIssue] =
    try {
      params.getParamList.asScala.foreach(p => logger.debug(s"        [Issue Parameter]:${p.getName}:${p.getValue}"))
      Right(Backlog4jConverters.Issue(backlog.importIssue(params)))
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        Left(e)
    }

  override def setCreateParam(projectId: Long,
                              propertyResolver: PropertyResolver,
                              toRemoteIssueId: (Long) => Option[Long],
                              issueOfId: (Long) => BacklogIssue)(backlogIssue: BacklogIssue): ImportIssueParams = {
    //issue type
    val issueTypeId = backlogIssue.optIssueTypeName match {
      case Some(issueTypeName) =>
        propertyResolver.optResolvedIssueTypeId(issueTypeName).getOrElse(propertyResolver.tryDefaultIssueTypeId())
      case None => propertyResolver.tryDefaultIssueTypeId()
    }

    //priority
    val priorityType = propertyResolver
      .optResolvedPriorityId(backlogIssue.priorityName)
      .map(value => PriorityType.valueOf(value.toInt))
      .getOrElse(PriorityType.Normal)

    //parameter
    val params: ImportIssueParams = new ImportIssueParams(projectId, backlogIssue.summary, issueTypeId, priorityType)

    //parent issue
    val optParentIssue: Option[BacklogIssue] = backlogIssue.optParentIssueId.flatMap(toRemoteIssueId(_).map(issueOfId))

    //description
    (backlogIssue.optParentIssueId, optParentIssue) match {
      case (Some(_), Some(parentIssue)) if (parentIssue.optParentIssueId.nonEmpty) =>
        val sb = new StringBuilder()
        sb.append(backlogIssue.description).append("\n")
        sb.append(Messages("common.parent_issue")).append(":").append(parentIssue.optIssueKey.getOrElse(""))
        params.description(sb.toString())
      case (Some(_), Some(parentIssue)) if (parentIssue.optParentIssueId.isEmpty) =>
        params.parentIssueId(parentIssue.id) //parent id
        params.description(backlogIssue.description)
      case _ =>
        params.description(backlogIssue.description)
    }

    //category
    val categoryIds = backlogIssue.categoryNames.flatMap(propertyResolver.optResolvedCategoryId)
    params.categoryIds(categoryIds.asJava)

    //version
    val versionIds = backlogIssue.versionNames.flatMap(propertyResolver.optResolvedVersionId)
    params.versionIds(versionIds.asJava)

    //milestone
    val milestoneIds = backlogIssue.milestoneNames.flatMap(propertyResolver.optResolvedVersionId)
    params.milestoneIds(milestoneIds.asJava)

    //assignee
    for {
      user   <- backlogIssue.optAssignee
      userId <- user.optUserId
      id     <- propertyResolver.optResolvedUserId(userId)
    } yield params.assigneeId(id)

    //start date
    for { startDate <- backlogIssue.optStartDate } yield params.startDate(startDate)

    //due date
    for { dueDate <- backlogIssue.optDueDate } yield params.dueDate(dueDate)

    //estimated hours
    for (estimatedHours <- backlogIssue.optEstimatedHours) yield params.estimatedHours(estimatedHours)

    //actual hours
    for { actualHours <- backlogIssue.optActualHours } yield params.actualHours(actualHours)

    //created
    for { created <- backlogIssue.operation.optCreated } yield params.created(created)

    //created user id
    for {
      createdUser <- backlogIssue.operation.optCreatedUser
      userId      <- createdUser.optUserId
      id          <- propertyResolver.optResolvedUserId(userId)
    } yield params.createdUserId(id)

    //updated
    for { updated <- backlogIssue.operation.optUpdated } yield params.updated(updated)

    //updated user id
    for {
      updatedUser <- backlogIssue.operation.optUpdatedUser
      userId      <- updatedUser.optUserId
      id          <- propertyResolver.optResolvedUserId(userId)
    } yield params.updatedUserId(id)

    //custom fields
    backlogIssue.customFields.map(setCustomFieldParams).foreach(_(params, propertyResolver))

    //notified user id
    val notifiedUserIds = backlogIssue.notifiedUsers.flatMap(_.optUserId).flatMap(propertyResolver.optResolvedUserId)
    params.notifiedUserIds(notifiedUserIds.asJava)
    params
  }

  override def createDummy(projectId: Long, propertyResolver: PropertyResolver): Issue = {
    //parameter
    val params: ImportIssueParams = new ImportIssueParams(
      projectId,
      "dummy issue",
      propertyResolver.tryDefaultIssueTypeId(),
      PriorityType.Normal
    )
    val issue = backlog.importIssue(params)
    logger.debug(
      s"[Success Finish Create Dummy Issue]:${issue.getId}----------------------------"
    )
    issue
  }

  override def delete(issueId: Long) = backlog.deleteIssue(issueId)

  override def addIssuesParams(params: GetIssuesParams, filter: Option[String]) =
    for { queryString <- filter } yield {
      val newQueryString =
        if (queryString.startsWith("?")) queryString else s"?$queryString"
      val uri = Uri.parse(newQueryString)
      for { issueTypeIds <- uri.query.paramMap.get("issueTypeId[]") } yield {
        params.issueTypeIds(issueTypeIds.asJava)
      }
      for { categoryIds <- uri.query.paramMap.get("categoryId[]") } yield {
        params.categoryIds(categoryIds.asJava)
      }
      for { versionIds <- uri.query.paramMap.get("versionId[]") } yield {
        params.versionIds(versionIds.asJava)
      }
      for { milestoneIds <- uri.query.paramMap.get("milestoneId[]") } yield {
        params.milestoneIds(milestoneIds.asJava)
      }
      for { statusIds <- uri.query.paramMap.get("statusId[]") } yield {
        params.statuses(statusIds.map(_.toInt).map(Issue.StatusType.valueOf).asJava)
      }
      for { priorityIds <- uri.query.paramMap.get("priorityId[]") } yield {
        params.priorities(priorityIds.map(_.toInt).map(Issue.PriorityType.valueOf).asJava)
      }
      for { assigneeIds <- uri.query.paramMap.get("assigneeId[]") } yield {
        params.assigneeIds(assigneeIds.asJava)
      }
      for { createdUserIds <- uri.query.paramMap.get("createdUserId[]") } yield {
        params.createdUserIds(createdUserIds.asJava)
      }
      for { resolutionIds <- uri.query.paramMap.get("resolutionId[]") } yield {
        params.resolutions(
          resolutionIds.map(_.toInt).map(Issue.ResolutionType.valueOf).asJava
        )
      }
      for {
        parentChild <- uri.query.paramMap.get("parentChild")
        head        <- parentChild.headOption
      } yield {
        if (head == "0") params.parentChildType(GetIssuesParams.ParentChildType.All)
        else if (head == "1") params.parentChildType(GetIssuesParams.ParentChildType.NotChild)
        else if (head == "2") params.parentChildType(GetIssuesParams.ParentChildType.Child)
        else if (head == "3") params.parentChildType(GetIssuesParams.ParentChildType.NotChildNotParent)
        else if (head == "4") params.parentChildType(GetIssuesParams.ParentChildType.Parent)
      }
      for {
        attachment <- uri.query.paramMap.get("attachment")
        head       <- attachment.headOption
      } yield params.attachment(head.toBoolean)
      for {
        sharedFile <- uri.query.paramMap.get("sharedFile")
        head       <- sharedFile.headOption
      } yield params.sharedFile(head.toBoolean)
      for {
        createdSince <- uri.query.paramMap.get("createdSince")
        head         <- createdSince.headOption
      } yield params.createdSince(head)
      for {
        createdUntil <- uri.query.paramMap.get("createdUntil")
        head         <- createdUntil.headOption
      } yield params.createdUntil(head)
      for {
        updatedUntil <- uri.query.paramMap.get("updatedUntil")
        head         <- updatedUntil.headOption
      } yield params.updatedUntil(head)
      for {
        startDateSince <- uri.query.paramMap.get("startDateSince")
        head           <- startDateSince.headOption
      } yield params.startDateSince(head)
      for {
        dueDateSince <- uri.query.paramMap.get("dueDateSince")
        head         <- dueDateSince.headOption
      } yield params.dueDateSince(head)
      for {
        dueDateUntil <- uri.query.paramMap.get("dueDateUntil")
        head         <- dueDateUntil.headOption
      } yield params.dueDateUntil(head)
      for { ids <- uri.query.paramMap.get("id[]") } yield {
        params.ids(ids.asJava)
      }
      for { parentIssueIds <- uri.query.paramMap.get("parentIssueId[]") } yield {
        params.parentIssueIds(parentIssueIds.asJava)
      }
      for {
        keyword <- uri.query.paramMap.get("keyword")
        head    <- keyword.headOption
      } yield params.keyword(head)
      params.sort(GetIssuesParams.SortKey.Created)
      params.order(GetIssuesParams.Order.Asc)
      params
    }

  override def addIssuesCountParams(params: GetIssuesCountParams, filter: Option[String]) =
    for { queryString <- filter } yield {
      val newQueryString =
        if (queryString.startsWith("?")) queryString else s"?$queryString"
      val uri = Uri.parse(newQueryString)
      for { issueTypeIds <- uri.query.paramMap.get("issueTypeId[]") } yield {
        params.issueTypeIds(issueTypeIds.asJava)
      }
      for { categoryIds <- uri.query.paramMap.get("categoryId[]") } yield {
        params.categoryIds(categoryIds.asJava)
      }
      for { versionIds <- uri.query.paramMap.get("versionId[]") } yield {
        params.versionIds(versionIds.asJava)
      }
      for { milestoneIds <- uri.query.paramMap.get("milestoneId[]") } yield {
        params.milestoneIds(milestoneIds.asJava)
      }
      for { statusIds <- uri.query.paramMap.get("statusId[]") } yield {
        params.statuses(statusIds.map(_.toInt).map(Issue.StatusType.valueOf).asJava)
      }
      for { priorityIds <- uri.query.paramMap.get("priorityId[]") } yield {
        params.priorities(priorityIds.map(_.toInt).map(Issue.PriorityType.valueOf).asJava)
      }
      for { assigneeIds <- uri.query.paramMap.get("assigneeId[]") } yield {
        params.assignerIds(assigneeIds.asJava)
      }
      for { createdUserIds <- uri.query.paramMap.get("createdUserId[]") } yield {
        params.createdUserIds(createdUserIds.asJava)
      }
      for { resolutionIds <- uri.query.paramMap.get("resolutionId[]") } yield {
        params.resolutions(
          resolutionIds.map(_.toInt).map(Issue.ResolutionType.valueOf).asJava
        )
      }
      for {
        parentChild <- uri.query.paramMap.get("parentChild")
        head        <- parentChild.headOption
      } yield {
        if (head == "0") params.parentChildType(GetIssuesCountParams.ParentChildType.All)
        else if (head == "1") params.parentChildType(GetIssuesCountParams.ParentChildType.NotChild)
        else if (head == "2") params.parentChildType(GetIssuesCountParams.ParentChildType.Child)
        else if (head == "3") params.parentChildType(GetIssuesCountParams.ParentChildType.NotChildNotParent)
        else if (head == "4") params.parentChildType(GetIssuesCountParams.ParentChildType.Parent)
      }
      for {
        attachment <- uri.query.paramMap.get("attachment")
        head       <- attachment.headOption
      } yield params.attachment(head.toBoolean)
      for {
        sharedFile <- uri.query.paramMap.get("sharedFile")
        head       <- sharedFile.headOption
      } yield params.sharedFile(head.toBoolean)
      for {
        createdSince <- uri.query.paramMap.get("createdSince")
        head         <- createdSince.headOption
      } yield params.createdSince(head)
      for {
        createdUntil <- uri.query.paramMap.get("createdUntil")
        head         <- createdUntil.headOption
      } yield params.createdUntil(head)
      for {
        updatedUntil <- uri.query.paramMap.get("updatedUntil")
        head         <- updatedUntil.headOption
      } yield params.updatedUntil(head)
      for {
        startDateSince <- uri.query.paramMap.get("startDateSince")
        head           <- startDateSince.headOption
      } yield params.startDateSince(head)
      for {
        dueDateSince <- uri.query.paramMap.get("dueDateSince")
        head         <- dueDateSince.headOption
      } yield params.dueDateSince(head)
      for {
        dueDateUntil <- uri.query.paramMap.get("dueDateUntil")
        head         <- dueDateUntil.headOption
      } yield params.dueDateUntil(head)
      for { ids <- uri.query.paramMap.get("id[]") } yield {
        params.ids(ids.asJava)
      }
      for { parentIssueIds <- uri.query.paramMap.get("parentIssueId[]") } yield {
        params.parentIssueIds(parentIssueIds.asJava)
      }
      for {
        keyword <- uri.query.paramMap.get("keyword")
        head    <- keyword.headOption
      } yield params.keyword(head)
    }

  private[this] def setCustomFieldParams(customField: BacklogCustomField)(params: CreateIssueParams, propertyResolver: PropertyResolver): Unit =
    for {
      customFieldSetting <- propertyResolver.optResolvedCustomFieldSetting(customField.name)
    } yield setCustomFieldParams(customField, params, customFieldSetting)

  private[this] def setCustomFieldParams(customField: BacklogCustomField,
                                         params: CreateIssueParams,
                                         customFieldSetting: BacklogCustomFieldSetting): Unit = {
    val typeId: Int = customFieldSetting.typeId
    FieldType.valueOf(typeId) match {
      case FieldType.Text         => setTextCustomField(customField, params, customFieldSetting)
      case FieldType.TextArea     => setTextAreaCustomField(customField, params, customFieldSetting)
      case FieldType.Numeric      => setNumericCustomField(customField, params, customFieldSetting)
      case FieldType.Date         => setDateCustomField(customField, params, customFieldSetting)
      case FieldType.SingleList   => setSingleListCustomField(customField, params, customFieldSetting)
      case FieldType.MultipleList => setMultipleListCustomField(customField, params, customFieldSetting)
      case FieldType.CheckBox     => setCheckBoxCustomField(customField, params, customFieldSetting)
      case FieldType.Radio        => setRadioCustomField(customField, params, customFieldSetting)
      case _                      =>
    }
  }

  private[this] def setTextCustomField(customField: BacklogCustomField, params: CreateIssueParams, customFieldSetting: BacklogCustomFieldSetting) = {
    for {
      value <- customField.optValue
      id    <- customFieldSetting.optId
    } yield params.textCustomField(id, value)
  }

  private[this] def setTextAreaCustomField(customField: BacklogCustomField,
                                           params: CreateIssueParams,
                                           customFieldSetting: BacklogCustomFieldSetting) = {
    for {
      value <- customField.optValue
      id    <- customFieldSetting.optId
    } yield params.textAreaCustomField(id, value)
  }

  private[this] def setDateCustomField(customField: BacklogCustomField, params: CreateIssueParams, customFieldSetting: BacklogCustomFieldSetting) = {
    for {
      value <- customField.optValue
      id    <- customFieldSetting.optId
    } yield params.dateCustomField(id, value)
  }

  private[this] def setNumericCustomField(customField: BacklogCustomField,
                                          params: CreateIssueParams,
                                          customFieldSetting: BacklogCustomFieldSetting) = {
    for {
      value <- customField.optValue
      id    <- customFieldSetting.optId
    } yield if (value.nonEmpty) params.numericCustomField(id, value.toFloat)
  }

  private[this] def setRadioCustomField(customField: BacklogCustomField, params: CreateIssueParams, customFieldSetting: BacklogCustomFieldSetting) = {
    (customField.optValue, customFieldSetting.property, customFieldSetting.optId) match {
      case (Some(value), property: BacklogCustomFieldMultipleProperty, Some(id)) =>
        for {
          item   <- property.items.find(_.name == value)
          itemId <- item.optId
        } yield params.radioCustomField(id, itemId)
      case _ =>
    }
  }

  private[this] def setSingleListCustomField(customField: BacklogCustomField,
                                             params: CreateIssueParams,
                                             customFieldSetting: BacklogCustomFieldSetting) = {
    (customField.optValue, customFieldSetting.property, customFieldSetting.optId) match {
      case (Some(value), property: BacklogCustomFieldMultipleProperty, Some(id)) =>
        for {
          item   <- property.items.find(_.name == value)
          itemId <- item.optId
        } yield params.singleListCustomField(id, itemId)
      case _ =>
    }
  }

  private[this] def setMultipleListCustomField(customField: BacklogCustomField,
                                               params: CreateIssueParams,
                                               customFieldSetting: BacklogCustomFieldSetting) = {
    (customFieldSetting.property, customFieldSetting.optId) match {
      case (property: BacklogCustomFieldMultipleProperty, Some(id)) =>
        def findItem(value: String): Option[BacklogItem] = {
          property.items.find(_.name == value)
        }
        def isItem(value: String): Boolean = {
          findItem(value).isDefined
        }
        val listItems   = customField.values.filter(isItem)
        val stringItems = customField.values.filterNot(isItem)

        val itemIds = listItems.flatMap(findItem).flatMap(_.optId)
        params.multipleListCustomField(id, itemIds.map(Long.box).asJava)
        params.customFieldOtherValue(id, stringItems.mkString(","))
      case _ =>
    }
  }

  private[this] def setCheckBoxCustomField(customField: BacklogCustomField,
                                           params: CreateIssueParams,
                                           customFieldSetting: BacklogCustomFieldSetting) = {
    (customFieldSetting.property, customFieldSetting.optId) match {
      case (property: BacklogCustomFieldMultipleProperty, Some(id)) =>
        def findItem(value: String): Option[BacklogItem] = {
          property.items.find(_.name == value)
        }
        def isItem(value: String): Boolean = {
          findItem(value).isDefined
        }
        val listItems   = customField.values.filter(isItem)
        val stringItems = customField.values.filterNot(isItem)

        val itemIds = listItems.flatMap(findItem).flatMap(_.optId)
        params.checkBoxCustomField(id, itemIds.map(Long.box).asJava)
        params.customFieldOtherValue(id, stringItems.mkString(","))
      case _ =>
    }
  }

}
