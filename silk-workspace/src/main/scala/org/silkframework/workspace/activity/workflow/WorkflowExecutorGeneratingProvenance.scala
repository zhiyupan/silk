package org.silkframework.workspace.activity.workflow

import java.util.logging.Logger

import org.silkframework.config.PlainTask
import org.silkframework.execution.ExecutionReport
import org.silkframework.runtime.activity._
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.reports.{ExecutionReportManager, ReportIdentifier}

/**
  * Executes a workflow child activity and generates provenance data (PROV-O) and writes it into the backend.
  */
trait WorkflowExecutorGeneratingProvenance extends Activity[WorkflowExecutionReportWithProvenance] {
  def workflowTask: ProjectTask[Workflow]
  private val log = Logger.getLogger(getClass.getName)

  /** The activity that executes the workflow and produces a workflow execution report */
  def workflowExecutionActivity(): Activity[WorkflowExecutionReport]

  override def run(context: ActivityContext[WorkflowExecutionReportWithProvenance])
                  (implicit userContext: UserContext): Unit = {
    val workflowExecutor: Activity[WorkflowExecutionReport] = workflowExecutionActivity()
    val control = context.child(workflowExecutor, 0.95)
    try {
      log.fine("Start child workflow executor activity")
      // Propagate workflow execution report
      val listener = (executionReport: WorkflowExecutionReport) => {
        context.value.update(WorkflowExecutionReportWithProvenance(executionReport, WorkflowExecutionProvenanceData(ActivityExecutionMetaData())))
      }
      control.value.subscribe(listener)
      control.start()
      control.waitUntilFinished()
    } finally {
      control.lastResult match {
        case Some(lastResult) =>
          val report = WorkflowExecutionReportWithProvenance.fromActivityExecutionReport(lastResult)
          context.value.update(report)
          ExecutionReportManager().addReport(ReportIdentifier.create(workflowTask.project.name, workflowTask.id), lastResult)
          val persistProvenanceService = PluginRegistry.createFromConfig[PersistWorkflowProvenance]("provenance.persistWorkflowProvenancePlugin")
          persistProvenanceService.persistWorkflowProvenance(workflowTask, lastResult)
        case None =>
          throw new RuntimeException("Child activity 'Execute local workflow' did not finish with result!")
      }
    }
  }
}

case class WorkflowExecutionReportWithProvenance(report: WorkflowExecutionReport, workflowExecutionProvenance: WorkflowExecutionProvenanceData)

object WorkflowExecutionReportWithProvenance {
  def fromActivityExecutionReport(activityResult: ActivityExecutionResult[WorkflowExecutionReport]): WorkflowExecutionReportWithProvenance = {
    val workflowExecutionProvenance = WorkflowExecutionProvenanceData(activityResult.metaData)
    WorkflowExecutionReportWithProvenance(activityResult.resultValue.get, workflowExecutionProvenance)
  }

  val empty = WorkflowExecutionReportWithProvenance(
    report = WorkflowExecutionReport(PlainTask("emptyReport", Workflow())),
    workflowExecutionProvenance = WorkflowExecutionProvenanceData(ActivityExecutionMetaData())
  )
}

case class WorkflowExecutionProvenanceData(activityMetaData: ActivityExecutionMetaData)