export function useStatusMapping() {
  function statusClass(status: string) {
    const map: Record<string, string> = {
      PendingReview: 'pending-review',
      InProgress: 'in-progress',
      Approved: 'approved',
      Rejected: 'rejected',
      Abandoned: 'abandoned',
    }
    return map[status] || 'in-progress'
  }

  function statusLabel(status: string) {
    const map: Record<string, string> = {
      PendingReview: 'Pending Review',
      InProgress: 'In Progress',
      Approved: 'Approved',
      Rejected: 'Rejected',
      Abandoned: 'Abandoned',
    }
    return map[status] || status
  }

  return { statusClass, statusLabel }
}
