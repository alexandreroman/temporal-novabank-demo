import { getTemporalClient } from '../../../utils/temporal'

export default defineEventHandler(async (event) => {
  const id = getRouterParam(event, 'id')!
  const body = await readBody(event)
  const { outcome, reason } = body

  if (!outcome || !['Approved', 'Rejected'].includes(outcome)) {
    throw createError({ statusCode: 400, message: 'Invalid outcome. Must be "Approved" or "Rejected".' })
  }

  const client = await getTemporalClient()
  const handle = client.workflow.getHandle(`account-application-${id}`)
  await handle.signal('submitReviewDecision', { outcome, reason: reason || '' })

  return { success: true }
})
