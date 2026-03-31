import { getTemporalClient } from '../../../utils/temporal'

export default defineEventHandler(async (event) => {
  const id = getRouterParam(event, 'id')!
  const client = await getTemporalClient()
  const handle = client.workflow.getHandle(`account-application-${id}`)
  const state = (await handle.query('getFormState')) as any
  if (state.kyc) {
    const { workflowId: _, ...kyc } = state.kyc
    state.kyc = kyc
  }
  return state
})
