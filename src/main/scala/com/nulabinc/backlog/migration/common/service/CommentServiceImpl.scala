package com.nulabinc.backlog.migration.common.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.conf.BacklogConstantValue
import com.nulabinc.backlog.migration.common.convert.Convert
import com.nulabinc.backlog.migration.common.convert.writes.{CommentWrites, IssueWrites}
import com.nulabinc.backlog.migration.common.domain._
import com.nulabinc.backlog.migration.common.utils.{Logging, StringUtil}
import com.nulabinc.backlog4j.CustomField.FieldType
import com.nulabinc.backlog4j.Issue.{PriorityType, ResolutionType, StatusType}
import com.nulabinc.backlog4j._
import com.nulabinc.backlog4j.api.option.{ImportUpdateIssueParams, QueryParams, UpdateIssueParams}

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class CommentServiceImpl @Inject()(implicit val issueWrites: IssueWrites,
                                   implicit val commentWrites: CommentWrites,
                                   backlog: BacklogClient,
                                   issueService: IssueService)
    extends CommentService
    with Logging {

  private val SINGLE_LIST_CUSTOM_FIELD_NOT_SET = -1

  override def allCommentsOfIssue(issueId: Long): Seq[BacklogComment] = {
    val allCount = backlog.getIssueCommentCount(issueId)

    def loop(optMinId: Option[Long], comments: Seq[IssueComment], offset: Long): Seq[IssueComment] =
      if (offset < allCount) {
        val queryParams = new QueryParams()
        for { minId <- optMinId } yield {
          queryParams.minId(minId)
        }
        queryParams.count(100)
        queryParams.order(QueryParams.Order.Asc)
        val commentsPart = backlog.getIssueComments(issueId, queryParams).asScala
        val optLastId = for { lastComment <- commentsPart.lastOption } yield {
          lastComment.getId
        }
        loop(optLastId, comments union commentsPart, offset + 100)
      } else comments

    loop(None, Seq.empty[IssueComment], 0).sortWith((c1, c2) => c1.getCreated.before(c2.getCreated)).map(Convert.toBacklog(_))
  }

  override def update(setUpdateParam: BacklogComment => ImportUpdateIssueParams)(
      backlogComment: BacklogComment): Either[Throwable, BacklogComment] = {
    try {
      val noUpdate = updateIssue(setUpdateParam(backlogComment))
      if (noUpdate)
        logger.debug(
          s"    [Success Finish No Update Comment]:issueId[${backlogComment.optIssueId.getOrElse("")}] created[${backlogComment.optCreated.getOrElse("")}]----------------------------")
      else
        logger.debug(
          s"    [Success Finish Create Comment]:issueId[${backlogComment.optIssueId.getOrElse("")}] created[${backlogComment.optCreated.getOrElse("")}]----------------------------")
      Right(backlogComment)
    } catch {
      case e: Throwable =>
        logger.debug(
          s"    [Fail Finish Create Comment]:issueId[${backlogComment.optIssueId.getOrElse("")}] created[${backlogComment.optCreated.getOrElse("")}]----------------------------")
        Left(e)
    }
  }

  override def setUpdateParam(issueId: Long,
                              propertyResolver: PropertyResolver,
                              toRemoteIssueId: (Long) => Option[Long],
                              postAttachment: (String) => Option[Long])(backlogComment: BacklogComment): ImportUpdateIssueParams = {
    logger.debug(s"    [Start Create Comment][Comment Date]:issueId[${issueId}] created[${backlogComment.optCreated.getOrElse("")}]")

    val optCurrentIssue = issueService.optIssueOfId(issueId)
    val params          = new ImportUpdateIssueParams(issueId)

    //comment
    for { content <- backlogComment.optContent } yield {
      params.comment(content)
    }

    //notificationUserIds
    val notifiedUserIds = backlogComment.notifications.flatMap(_.optUser).flatMap(_.optUserId).flatMap(propertyResolver.optResolvedUserId)
    params.notifiedUserIds(notifiedUserIds.asJava)

    //created updated
    for { created <- backlogComment.optCreated } yield {
      params.created(created)
      params.updated(created)
    }

    //created updated user id
    for {
      createdUser <- backlogComment.optCreatedUser
      userId      <- createdUser.optUserId
      id          <- propertyResolver.optResolvedUserId(userId)
    } yield params.updatedUserId(id)

    //changelog
    backlogComment.changeLogs.foreach { changeLog =>
      setChangeLog(changeLog, params, toRemoteIssueId, propertyResolver, postAttachment, optCurrentIssue)
    }

    params
  }

  private[this] def updateIssue(params: ImportUpdateIssueParams): Boolean = {
    val paramList = params.getParamList.asScala
    paramList.foreach(p => logger.debug(s"        [Comment Parameter]:${p.getName}:${p.getValue}"))
    if (paramList.exists(p => p.getName == "created") &&
        paramList.exists(p => p.getName == "updated") &&
        paramList.exists(p => p.getName == "updatedUserId") &&
        paramList.size == 3) {
      logger.warn("No update item")
      true
    } else {
      Convert.toBacklog(backlog.importUpdateIssue(params))
      false
    }
  }

  private[this] def setChangeLog(changeLog: BacklogChangeLog,
                                 params: ImportUpdateIssueParams,
                                 toRemoteIssueId: (Long) => Option[Long],
                                 propertyResolver: PropertyResolver,
                                 postAttachment: (String) => Option[Long],
                                 optCurrentIssue: Option[Issue]) = {
    if (changeLog.optAttributeInfo.nonEmpty) {
      setCustomField(params, changeLog, propertyResolver)
    } else if (changeLog.optAttachmentInfo.nonEmpty) {
      setAttachment(params, changeLog, postAttachment)
    } else setAttr(params, changeLog, toRemoteIssueId, propertyResolver, optCurrentIssue)
  }

  private[this] def setAttr(params: ImportUpdateIssueParams,
                            changeLog: BacklogChangeLog,
                            toRemoteIssueId: (Long) => Option[Long],
                            propertyResolver: PropertyResolver,
                            optCurrentIssue: Option[Issue]) =
    changeLog.field match {
      case BacklogConstantValue.ChangeLog.SUMMARY         => setSummary(params, changeLog)
      case BacklogConstantValue.ChangeLog.DESCRIPTION     => setDescription(params, changeLog)
      case BacklogConstantValue.ChangeLog.COMPONENT       => setCategory(params, changeLog, propertyResolver)
      case BacklogConstantValue.ChangeLog.VERSION         => setVersion(params, changeLog, propertyResolver)
      case BacklogConstantValue.ChangeLog.MILESTONE       => setMilestone(params, changeLog, propertyResolver)
      case BacklogConstantValue.ChangeLog.STATUS          => setStatus(params, changeLog, propertyResolver, optCurrentIssue)
      case BacklogConstantValue.ChangeLog.ASSIGNER        => setAssignee(params, changeLog, propertyResolver)
      case BacklogConstantValue.ChangeLog.ISSUE_TYPE      => setIssueType(params, changeLog, propertyResolver, optCurrentIssue)
      case BacklogConstantValue.ChangeLog.START_DATE      => setStartDate(params, changeLog)
      case BacklogConstantValue.ChangeLog.LIMIT_DATE      => setDueDate(params, changeLog)
      case BacklogConstantValue.ChangeLog.PRIORITY        => setPriority(params, changeLog, propertyResolver)
      case BacklogConstantValue.ChangeLog.RESOLUTION      => setResolution(params, changeLog, propertyResolver)
      case BacklogConstantValue.ChangeLog.ESTIMATED_HOURS => setEstimatedHours(params, changeLog)
      case BacklogConstantValue.ChangeLog.ACTUAL_HOURS    => setActualHours(params, changeLog)
      case BacklogConstantValue.ChangeLog.PARENT_ISSUE    => setParentIssue(params, changeLog, toRemoteIssueId)
      case BacklogConstantValue.ChangeLog.NOTIFICATION    =>
      case BacklogConstantValue.ChangeLog.ATTACHMENT      =>
    }

  private[this] def setSummary(params: ImportUpdateIssueParams, changeLog: BacklogChangeLog) = {
    changeLog.optNewValue.map(value => params.summary(value))
  }

  private[this] def setDescription(params: ImportUpdateIssueParams, changeLog: BacklogChangeLog) =
    changeLog.optNewValue match {
      case Some(value) => params.description(value)
      case _           => params.description(null)
    }

  private[this] def setStartDate(params: ImportUpdateIssueParams, changeLog: BacklogChangeLog) =
    changeLog.optNewValue match {
      case Some(value) => params.startDate(value)
      case _           => params.startDate(null)
    }

  private[this] def setDueDate(params: ImportUpdateIssueParams, changeLog: BacklogChangeLog) =
    changeLog.optNewValue match {
      case Some(value) => params.dueDate(value)
      case _           => params.dueDate(null)
    }

  private[this] def setCategory(params: ImportUpdateIssueParams, changeLog: BacklogChangeLog, propertyResolver: PropertyResolver) =
    changeLog.optNewValue match {
      case Some("") => params.categoryIds(null)
      case Some(value) =>
        val ids = value.split(",").toSeq.map(_.trim).flatMap(propertyResolver.optResolvedCategoryId)
        if (ids.nonEmpty) params.categoryIds(ids.asJava) else params.categoryIds(null)
      case None => params.categoryIds(null)
    }

  private[this] def setVersion(params: ImportUpdateIssueParams, changeLog: BacklogChangeLog, propertyResolver: PropertyResolver) =
    changeLog.optNewValue match {
      case Some("") => params.versionIds(null)
      case Some(value) =>
        val ids = value.split(",").toSeq.flatMap(propertyResolver.optResolvedVersionId)
        if (ids.nonEmpty) params.versionIds(ids.asJava) else params.versionIds(null)
      case None => params.versionIds(null)
    }

  private[this] def setMilestone(params: ImportUpdateIssueParams, changeLog: BacklogChangeLog, propertyResolver: PropertyResolver) =
    changeLog.optNewValue match {
      case Some("") => params.milestoneIds(null)
      case Some(value) =>
        val ids = value.split(",").toSeq.flatMap(propertyResolver.optResolvedVersionId)
        if (ids.nonEmpty) params.milestoneIds(ids.asJava) else params.milestoneIds(null)
      case None => params.milestoneIds(null)
    }

  private[this] def setStatus(params: ImportUpdateIssueParams,
                              changeLog: BacklogChangeLog,
                              propertyResolver: PropertyResolver,
                              optCurrentIssue: Option[Issue]) = {
    (optCurrentIssue, changeLog.optNewValue) match {
      case (Some(currentIssue), Some(newValue)) =>
        val newStatusId = propertyResolver.tryResolvedStatusId(newValue)
        if (currentIssue.getStatus.getId != newStatusId) {
          if (currentIssue.getStatus.getId == StatusType.Closed.getIntValue) {
            if (newStatusId == StatusType.InProgress.getIntValue) {
              params.status(StatusType.valueOf(newStatusId))
            }
          } else {
            params.status(StatusType.valueOf(newStatusId))
          }
        }
      case _ =>
        for { value <- changeLog.optNewValue } yield params.status(StatusType.valueOf(propertyResolver.tryResolvedStatusId(value)))
    }
  }

  private[this] def setAssignee(params: ImportUpdateIssueParams, changeLog: BacklogChangeLog, propertyResolver: PropertyResolver): Object =
    changeLog.optNewValue match {
      case Some("") => params.assigneeId(-1L)
      case Some(value) =>
        for {
          id <- propertyResolver.optResolvedUserId(value)
        } yield params.assigneeId(id)
      case None => params.assigneeId(-1L)
    }

  private[this] def setIssueType(params: ImportUpdateIssueParams,
                                 changeLog: BacklogChangeLog,
                                 propertyResolver: PropertyResolver,
                                 optCurrentIssue: Option[Issue]) =
    for {
      value        <- changeLog.optNewValue
      currentIssue <- optCurrentIssue
    } yield {
      for { id <- propertyResolver.optResolvedIssueTypeId(value) } yield {
        if (id != currentIssue.getIssueType.getId) {
          params.issueTypeId(id)
        }
      }
    }

  private[this] def setPriority(params: ImportUpdateIssueParams, changeLog: BacklogChangeLog, propertyResolver: PropertyResolver) =
    for {
      value        <- changeLog.optNewValue
      priorityType <- propertyResolver.optResolvedPriorityId(value).map(value => PriorityType.valueOf(value.toInt))
    } yield params.priority(priorityType)

  private[this] def setResolution(params: ImportUpdateIssueParams, changeLog: BacklogChangeLog, propertyResolver: PropertyResolver) =
    for { value <- changeLog.optNewValue } yield {
      val optResolutionType = propertyResolver.optResolvedResolutionId(value).map(value => ResolutionType.valueOf(value.toInt))
      params.resolution(optResolutionType.getOrElse(ResolutionType.NotSet))
    }

  private[this] def setEstimatedHours(params: ImportUpdateIssueParams, changeLog: BacklogChangeLog) =
    changeLog.optNewValue match {
      case Some("")    => params.estimatedHours(null)
      case Some(value) => params.estimatedHours(value.toFloat)
      case None        => params.estimatedHours(null)
    }

  private[this] def setActualHours(params: ImportUpdateIssueParams, changeLog: BacklogChangeLog) =
    changeLog.optNewValue match {
      case Some("")    => params.actualHours(null)
      case Some(value) => params.actualHours(value.toFloat)
      case None        => params.actualHours(null)
    }

  private[this] def setParentIssue(params: ImportUpdateIssueParams, changeLog: BacklogChangeLog, toRemoteIssueId: (Long) => Option[Long]) =
    changeLog.optNewValue match {
      case Some(value) =>
        for {
          id <- toRemoteIssueId(value.toLong)
        } yield {
          val parentIssue = issueService.issueOfId(id)
          if (parentIssue.optParentIssueId.isEmpty) {
            params.parentIssueId(id)
          }
        }
      case None =>
        params.parentIssueId(UpdateIssueParams.PARENT_ISSUE_NOT_SET)
    }

  private[this] def setAttachment(params: ImportUpdateIssueParams, changeLog: BacklogChangeLog, postAttachment: (String) => Option[Long]) =
    for {
      fileName <- changeLog.optAttachmentInfo.map(_.name)
      id       <- postAttachment(fileName)
    } yield params.attachmentIds(Seq(Long.box(id)).asJava)

  private[this] def setCustomField(params: ImportUpdateIssueParams, changeLog: BacklogChangeLog, propertyResolver: PropertyResolver) =
    for { customFieldSetting <- propertyResolver.optResolvedCustomFieldSetting(changeLog.field) } yield {
      FieldType.valueOf(customFieldSetting.typeId) match {
        case FieldType.Text         => setTextCustomField(params, changeLog, customFieldSetting)
        case FieldType.TextArea     => setTextCustomFieldArea(params, changeLog, customFieldSetting)
        case FieldType.Numeric      => setNumericCustomField(params, changeLog, customFieldSetting)
        case FieldType.Date         => setDateCustomField(params, changeLog, customFieldSetting)
        case FieldType.SingleList   => setSingleListCustomField(params, changeLog, customFieldSetting)
        case FieldType.MultipleList => setMultipleListCustomField(params, changeLog, customFieldSetting)
        case FieldType.CheckBox     => setCheckBoxCustomField(params, changeLog, customFieldSetting)
        case FieldType.Radio        => setRadioCustomField(params, changeLog, customFieldSetting)
        case _                      =>
      }
    }

  private[this] def setTextCustomField(params: ImportUpdateIssueParams,
                                       changeLog: BacklogChangeLog,
                                       customFieldSetting: BacklogCustomFieldSetting) = {
    for {
      value <- changeLog.optNewValue
      id    <- customFieldSetting.optId
    } yield params.textCustomField(id, value)
  }

  private[this] def setTextCustomFieldArea(params: ImportUpdateIssueParams,
                                           changeLog: BacklogChangeLog,
                                           customFieldSetting: BacklogCustomFieldSetting) = {
    for {
      value <- changeLog.optNewValue
      id    <- customFieldSetting.optId
    } yield params.textAreaCustomField(id, value)
  }

  private[this] def setDateCustomField(params: ImportUpdateIssueParams, changeLog: BacklogChangeLog, customFieldSetting: BacklogCustomFieldSetting) =
    (changeLog.optNewValue, customFieldSetting.optId) match {
      case (Some(""), Some(id))    => params.dateCustomField(id, null)
      case (Some(value), Some(id)) => params.dateCustomField(id, value)
      case (None, Some(id))        => params.dateCustomField(id, null)
      case _                       => throw new RuntimeException
    }

  private[this] def setNumericCustomField(params: ImportUpdateIssueParams,
                                          changeLog: BacklogChangeLog,
                                          customFieldSetting: BacklogCustomFieldSetting) =
    (changeLog.optNewValue, customFieldSetting.optId) match {
      case (Some(""), Some(id))    => params.numericCustomField(id, null)
      case (Some(value), Some(id)) => params.numericCustomField(id, StringUtil.safeUnitStringToFloat(value))
      case (None, Some(id))        => params.numericCustomField(id, null)
      case _                       => throw new RuntimeException
    }

  private[this] def setCheckBoxCustomField(params: ImportUpdateIssueParams,
                                           changeLog: BacklogChangeLog,
                                           customFieldSetting: BacklogCustomFieldSetting) =
    (changeLog.optNewValue, customFieldSetting.property, customFieldSetting.optId) match {
      case (Some(value), property: BacklogCustomFieldMultipleProperty, Some(id)) =>
        val newValues: Seq[String] = value.split(",").toSeq.map(_.trim)

        def findItem(newValue: String): Option[BacklogItem] = {
          property.items.find(_.name == newValue)
        }

        def isItem(value: String): Boolean = {
          findItem(value).isDefined
        }
        val listItems   = newValues.filter(isItem)
        val stringItems = newValues.filterNot(isItem)

        val itemIds = listItems.flatMap(findItem).flatMap(_.optId)
        params.checkBoxCustomField(id, itemIds.map(Long.box).asJava)
        params.customFieldOtherValue(id, stringItems.mkString(","))
      case _ =>
    }

  private[this] def setRadioCustomField(params: ImportUpdateIssueParams, changeLog: BacklogChangeLog, customFieldSetting: BacklogCustomFieldSetting) =
    (changeLog.optNewValue, customFieldSetting.property, customFieldSetting.optId) match {
      case (Some(value), property: BacklogCustomFieldMultipleProperty, Some(id)) if (value.nonEmpty) =>
        for {
          item   <- property.items.find(_.name == value)
          itemId <- item.optId
        } yield params.radioCustomField(id, itemId)
      case _ =>
    }

  private[this] def setSingleListCustomField(params: ImportUpdateIssueParams,
                                             changeLog: BacklogChangeLog,
                                             customFieldSetting: BacklogCustomFieldSetting) =
    for { id <- customFieldSetting.optId } yield {
      (changeLog.optNewValue, customFieldSetting.property) match {
        case (Some(value), property: BacklogCustomFieldMultipleProperty) if (value.nonEmpty) =>
          for {
            item   <- property.items.find(_.name == value)
            itemId <- item.optId
          } yield params.singleListCustomField(id, itemId)
        case _ => params.singleListCustomField(id, SINGLE_LIST_CUSTOM_FIELD_NOT_SET)
      }
    }

  private[this] def setMultipleListCustomField(params: ImportUpdateIssueParams,
                                               changeLog: BacklogChangeLog,
                                               customFieldSetting: BacklogCustomFieldSetting) =
    (changeLog.optNewValue, customFieldSetting.property, customFieldSetting.optId) match {
      case (Some(value), property: BacklogCustomFieldMultipleProperty, Some(id)) =>
        val newValues: Seq[String] = value.split(",").toSeq.map(_.trim)

        def findItem(newValue: String): Option[BacklogItem] = {
          property.items.find(_.name == newValue)
        }

        def isItem(value: String): Boolean = {
          findItem(value).isDefined
        }
        val listItems   = newValues.filter(isItem)
        val stringItems = newValues.filterNot(isItem)

        val itemIds = listItems.flatMap(findItem).flatMap(_.optId)
        params.multipleListCustomField(id, itemIds.map(Long.box).asJava)
        params.customFieldOtherValue(id, stringItems.mkString(","))
      case _ =>
    }

}
