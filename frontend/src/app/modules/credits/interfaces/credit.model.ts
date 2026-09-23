export type CreditStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'

export interface CreditApplicant {
  id: number
  fullName: string
  email: string
}

export interface Credit {
  id: number
  applicant: CreditApplicant
  amount: number
  termMonths: number
  status: CreditStatus
  statusLabel: string
  /** true: tasa y cuota estimadas con la tasa sugerida (aún no aprobado) */
  estimated: boolean
  suggestedAnnualRate: number
  annualRate: number
  monthlyRate: number
  monthlyPayment: number
  totalInterest: number
  totalPayable: number
  rejectionReason: string | null
  createdAt: string
  decidedAt: string | null
}

export interface AmortizationRow {
  period: number
  payment: number
  interest: number
  principal: number
  balance: number
}

export interface CreditQuote {
  amount: number
  termMonths: number
  annualRate: number
  monthlyRate: number
  monthlyPayment: number
  totalInterest: number
  totalPayable: number
  schedule: AmortizationRow[]
}

export interface InterestRateTier {
  id: number
  name: string
  minTermMonths: number
  maxTermMonths: number
  annualEffectiveRate: number
}

/** Tramos de tasa + límites de la política (fuente de las validaciones del front). */
export interface RateCatalog {
  minAnnualRate: number
  maxAnnualRate: number
  minAmount: number
  maxAmount: number
  minTermMonths: number
  maxTermMonths: number
  tiers: InterestRateTier[]
}

export interface CreateCreditRequest {
  amount: number
  termMonths: number
}

export interface SimulateCreditRequest {
  amount: number
  termMonths: number
  annualRate?: number | null
  includeSchedule?: boolean
}
