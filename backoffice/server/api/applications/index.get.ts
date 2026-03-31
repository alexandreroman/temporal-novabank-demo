import { getTemporalClient, WORKFLOW_TYPE } from '../../utils/temporal'

export default defineEventHandler(async (event) => {
  const query = getQuery(event)
  const statusFilter = query.status as string | undefined

  const client = await getTemporalClient()

  let queryStr = `WorkflowType = '${WORKFLOW_TYPE}'`
  if (statusFilter) {
    queryStr += ` AND ReviewStatus = '${statusFilter}'`
  }
  queryStr += ' ORDER BY StartTime DESC'

  const applications: any[] = []
  const workflows = client.workflow.list({ query: queryStr })
  for await (const workflow of workflows) {
    const searchAttrs = workflow.searchAttributes
    applications.push({
      workflowId: workflow.workflowId,
      applicationId: workflow.workflowId.replace('account-application-', ''),
      status: workflow.status.name,
      startTime: workflow.startTime?.toISOString(),
      reviewStatus: searchAttrs?.ReviewStatus?.[0] ?? 'Unknown',
      kycStatus: searchAttrs?.KycStatus?.[0] ?? 'Unknown',
      applicantName: searchAttrs?.ApplicantName?.[0] ?? 'Unknown',
    })
  }

  return applications
})
