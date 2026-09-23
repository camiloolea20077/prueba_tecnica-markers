import { CreditStatus } from '../../../credits/interfaces/credit.model'

export interface CreditSummary {
  pending: number
  approved: number
  rejected: number
  cancelled: number
  total: number
}

export interface AdminCreditQuery {
  status: CreditStatus | null
  q: string
  page: number
  size: number
}
