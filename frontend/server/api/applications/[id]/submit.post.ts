import { getTemporalClient } from '../../../utils/temporal'

export default defineEventHandler(async (event) => {
  const id = getRouterParam(event, 'id')!
  const client = await getTemporalClient()
  const handle = client.workflow.getHandle(`account-application-${id}`)
  await handle.signal('submitFinalForm')
  return { ok: true }
})
