import { getTemporalClient } from '../../../../utils/temporal'

export default defineEventHandler(async (event) => {
  const id = getRouterParam(event, 'id')!
  const page = getRouterParam(event, 'page')!
  const body = await readBody(event)
  const client = await getTemporalClient()
  const handle = client.workflow.getHandle(`account-application-${id}`)
  await handle.signal(`submitPage${page}`, body)
  return { ok: true }
})
