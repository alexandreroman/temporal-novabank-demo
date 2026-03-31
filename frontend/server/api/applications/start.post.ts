import { randomBytes } from 'node:crypto'
import { getTemporalClient, TASK_QUEUE, WORKFLOW_TYPE } from '../../utils/temporal'

export default defineEventHandler(async () => {
  const client = await getTemporalClient()
  const applicationId = randomBytes(4).toString('hex')
  const workflowId = `account-application-${applicationId}`

  await client.workflow.start(WORKFLOW_TYPE, {
    taskQueue: TASK_QUEUE,
    workflowId,
    args: [applicationId],
  })

  return { applicationId }
})
