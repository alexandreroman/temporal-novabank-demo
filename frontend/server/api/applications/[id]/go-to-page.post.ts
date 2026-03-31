import { getTemporalClient } from '../../../utils/temporal'

export default defineEventHandler(async (event) => {
  const id = getRouterParam(event, 'id')!
  const body = await readBody(event)
  const client = await getTemporalClient()
  const handle = client.workflow.getHandle(`account-application-${id}`)
  await handle.signal('goToPage', body.page)
  return { ok: true }
})
